package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import org.multij.Module;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprProc;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.type.CallableType;
import streamblocks.opencl.backend.emitters.Callables;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.Expressions;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

import java.util.ArrayList;
import java.util.List;

@Module
public interface OpenCLCallables extends Callables {

    default Emitter emitter() {
        return backend().clEmitter();
    }

    default Declarations declarations() {
        return backend().clDeclarations();
    }

    default OpenCLStatements statements() {
        return backend().clStatements();
    }

    default TypesEvaluator typeseval() {
        return backend().clTypeseval();
    }

    default Expressions expressions() {
        return backend().clExpressions();
    }

    @Override
    default void callableDefinition(String instanceName, ExprLambda lambda) {
        emitter().emit("%s {", lambdaHeader(instanceName, lambda,false));
        emitter().increaseIndentation();
        emitter().emit("return %s;", expressions().evaluate(lambda.getBody()));
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    @Override
    default void callableDefinition(String instanceName, ExprProc proc) {
        emitter().emit("%s {", procHeader(instanceName, proc,false));
        emitter().increaseIndentation();
        proc.getBody().forEach(statements()::execute);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    @Override
    default String callableHeader(String instanceName, String name, CallableType type, List<String> parameterNames) {
        List<String> parameters = new ArrayList<>();
        assert parameterNames.size() == type.getParameterTypes().size();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.add(declarations().declarationParameter(type.getParameterTypes().get(i), parameterNames.get(i)));
        }
        String result = typeseval().type(type.getReturnType());
        result += " ";
        result += name;
        result += "(";
        result += String.join(", ", parameters);
        result += ")";
        return result;
    }

    @Override
    default void callablePrototypes(String instanceName, ExprLambda lambda) {
        emitter().emit("%s;", lambdaHeader(instanceName, lambda, false));
    }

    @Override
    default void callablePrototypes(String instanceName, ExprProc proc) {
        emitter().emit("%s;", procHeader(instanceName, proc, false));
    }


}
