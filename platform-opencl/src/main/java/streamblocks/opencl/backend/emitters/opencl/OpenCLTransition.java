package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import ch.epfl.vlsc.platformutils.PathUtils;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.attribute.Types;
import se.lth.cs.tycho.ir.Annotation;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.Scope;
import se.lth.cs.tycho.ir.entity.am.Transition;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.ExprProc;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.type.Type;
import streamblocks.opencl.backend.OpenCLBackend;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.Expressions;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Module
public interface OpenCLTransition {

    String ACC_ANNOTATION = "acc";

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();


    default Emitter emitter() {
        return backend().clEmitter();
    }

    default OpenCLStatements statements() {
        return backend().clStatements();
    }

    default Declarations declarations() {
        return backend().clDeclarations();
    }

    default TypesEvaluator typeseval() {
        return backend().clTypeseval();
    }

    default Types types() {
        return backend().types();
    }

    default boolean anyAccTransition(ActorMachine actor) {

        for (int i = 0; i < actor.getTransitions().size(); i++) {
            if (Annotation.hasAnnotationWithName(ACC_ANNOTATION, actor.getTransitions().get(i).getAnnotations())) {
                return true;
            }
        }
        return false;
    }

    default void clOpenFile(String instanceName){
        Path instanceTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve(instanceName + ".cl");
        emitter().open(instanceTarget);
    }

    default void setupCLKernel(String instanceName, ActorMachine actor){
        Path instanceTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve(instanceName + ".cl");
        emitter().open(instanceTarget);

        emitter().emit("// -- Callables");
        for (Scope scope : actor.getScopes()) {
            if (scope.isPersistent()) {
                for (VarDecl decl : scope.getDeclarations()) {
                    if (decl.getValue() != null) {
                        Expression expr = decl.getValue();
                        if (expr instanceof ExprLambda || expr instanceof ExprProc) {
                            backend().clCallables().callablePrototypes(instanceName, decl.getValue());
                            backend().clCallables().callableDefinition(instanceName, expr);
                            emitter().emitNewLine();
                        }
                        /*else{
                            if(decl.getValue() instanceof ExprList){
                                String s = "__constant " + backend().clExpressions().evaluate(decl.getValue()) + ";\n";
                                emitter().emitRawLine(s.replace("$$", backend().variables().declarationName(decl)));
                                emitter().emitNewLine();
                            }
                            else{
                                String declare = declarations().declaration(types().declaredType(decl), backend().variables().declarationName(decl));
                                emitter().emit("__constant %s = %s;", declare, backend().clExpressions().evaluate(decl.getValue()));//backend().clExpressions().evaluate(decl.getValue());
                            }
                        }*/

                    }
                }
            }
        }
    }

    default void clCloseFile(){
        emitter().close();
    }

    default void emitScopes(ImmutableList<Scope> scopes){
        emitter().emit("// -- Scopes");
        for (Scope scope : scopes) {
            if (scope.isPersistent()) {
                for (VarDecl decl : scope.getDeclarations()) {
                    if (decl.getValue() != null) {
                        if (decl.getValue() instanceof ExprLambda || decl.getValue() instanceof ExprProc) {
                            //do nothing here
                        }
                        else if(decl.getValue() instanceof ExprList){
                            //emitter().emit("{");
                            emitter().increaseIndentation();

                            String s = "\t" + backend().clExpressions().evaluate(decl.getValue()) + ";\n";
                            emitter().emitRawLine(s.replace("$$", backend().variables().declarationName(decl)));
                            emitter().emitNewLine();

                            emitter().decreaseIndentation();
                            //emitter().emit("}");
                        }
                        else{
                            String declare = declarations().declaration(types().declaredType(decl), backend().variables().declarationName(decl));
                            emitter().emit("%s = %s;", declare, backend().clExpressions().evaluate(decl.getValue()));//backend().clExpressions().evaluate(decl.getValue());
                        }
                    }
                }
            }
        }
    }


    default void acceleratedTransition(String instanceName, Transition transition, int index, ImmutableList<Scope> scopes) {

        String name = "transition$" + index;
        String clTextHead = "";
        String clTextIndices = "";
        String portString = "";

        /*Path instanceTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve(instanceName + ".cl");
        emitter().open(instanceTarget);*/

        for (Map.Entry<Port, Integer> entry : transition.getInputRates().entrySet()) {
            portString = entry.getKey().getName();
            clTextHead += "__global " + typeseval().type(backend().types().portType(entry.getKey())) + "* " + portString + "$FIFO$ptr, ";
            clTextIndices += "int " + portString + "$FIFO$index =  get_global_id(0) * " + entry.getValue() +";\n\t";
        }
        for (Map.Entry<Port, Integer> entry : transition.getOutputRates().entrySet()) {
            portString = entry.getKey().getName();
            clTextHead += "__global " + typeseval().type(backend().types().portType(entry.getKey())) + "* " + portString + "$FIFO$ptr, ";
            clTextIndices += "int " + portString + "$FIFO$index =  get_global_id(0) * " + entry.getValue() +";\n\t";
        }

        clTextHead = clTextHead.replaceAll(", $", "");
        clTextIndices = clTextIndices.replaceAll("\n\t$", "");


        emitter().emit("__kernel void %s(%s) {", name, clTextHead);

        {
            emitter().increaseIndentation();

            emitter().emit("%s", clTextIndices);

            emitScopes(scopes);

            transition.getBody().forEach(statements()::execute);

            emitter().decreaseIndentation();
        }

        emitter().emit("}");
        //emitter().close();
    }
}
