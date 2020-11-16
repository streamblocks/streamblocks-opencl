package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import org.multij.Module;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.StmtBlock;
import se.lth.cs.tycho.ir.stmt.StmtConsume;
import se.lth.cs.tycho.ir.stmt.StmtForeach;
import se.lth.cs.tycho.ir.stmt.StmtWrite;
import se.lth.cs.tycho.type.ListType;
import se.lth.cs.tycho.type.Type;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.Expressions;
import streamblocks.opencl.backend.emitters.Statements;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Module
public interface OpenCLStatements extends Statements {

    @Override
    default Emitter emitter() {
        return backend().clEmitter();
    }

    @Override
    default Declarations declarations() {
        return backend().clDeclarations();
    }

    @Override
    default TypesEvaluator typeseval() {
        return backend().clTypeseval();
    }

    default Expressions expressions() {
        return backend().clExpressions();
    }

    @Override
    default void execute(StmtConsume consume) {
        //emitter().emit("%s$FIFO.dontconsume(%s);", consume.getPort().getName(), consume.getNumberOfTokens());
    }

    @Override
    default void execute(StmtWrite write) {
        String portName = write.getPort().getName();
        if (write.getRepeatExpression() == null) {
            String tmp = variables().generateTemp();
            emitter().emit("%s;", declarations().declaration(types().portType(write.getPort()), tmp));
            for (Expression expr : write.getValues()) {
                String s = tmp + " = " + expressions().evaluate(expr);
                s = s.replace("this->", "");
                emitter().emit("%s;", s);
                //emitter().emit("%s = %s;", tmp, expressions().evaluate(expr));
                emitter().emit("%s$FIFO$ptr[%s$FIFO$index++] = %s;", portName, portName, tmp);
            }
        } else if (write.getValues().size() == 1) {
            String value = expressions().evaluate(write.getValues().get(0));
            String repeat = expressions().evaluate(write.getRepeatExpression());
            Type t = types().portType(write.getPort());
            clcopy(portName, value, repeat);
            //emitter().emit("%s$FIFO.put_elements(%s, %s);", portName, value, repeat);
        } else {
            throw new Error("not implemented");
        }
    }

    @Override
    default void execute(StmtBlock block) {
        emitter().emit("{");
        emitter().increaseIndentation();

        //backend().callables().declareEnvironmentForCallablesInScope(block);
        for (VarDecl decl : block.getVarDecls()) {
            Type t = types().declaredType(decl);
            String declarationName = variables().declarationName(decl);
            String d = declarations().declaration(t, declarationName);
            emitter().emit("%s = %s;", d, backend().defaultValues().defaultValue(t));
            if (decl.getValue() != null) {
                copy(t, declarationName, types().type(decl.getValue()), expressions().evaluate(decl.getValue()));
            }
        }
        block.getStatements().forEach(this::execute);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    @Override
    default void execute(StmtForeach foreach) {
        forEach(foreach.getGenerator().getCollection(), foreach.getGenerator().getVarDecls(), () -> {
            for (Expression filter : foreach.getFilters()) {
                emitter().emit("if (%s) {", expressions().evaluate(filter));
                emitter().increaseIndentation();
            }
            foreach.getBody().forEach(this::execute);
            for (Expression filter : foreach.getFilters()) {
                emitter().decreaseIndentation();
                emitter().emit("}");
            }
        });
    }

    @Override
    default void copy(Type lvalueType, String lvalue, Type rvalueType, String rvalue) {
        emitter().emit("%s = %s;", lvalue, rvalue);
    }

    @Override
    default void copy(ListType lvalueType, String lvalue, ListType rvalueType, String rvalue) {

        String index = variables().generateTemp();
        emitter().emit("for (int %1$s = 0; %1$s < %2$s; %1$s++) {", index, lvalueType.getSize().getAsInt());
        emitter().increaseIndentation();
        Pattern p = Pattern.compile("\\b\\$FIFO\\$ptr\\b");
        Matcher m = p.matcher(rvalue);
        if(m.find())
            copy(lvalueType.getElementType(), String.format("%s[%s]", lvalue, index), rvalueType.getElementType(), String.format("%s", rvalue));
        else copy(lvalueType.getElementType(), String.format("%s[%s]", lvalue, index), rvalueType.getElementType(), String.format("%s[%s]", rvalue, index));
        emitter().decreaseIndentation();
        emitter().emit("}");

    }

    default void clcopy(String pname, String value, String repeat) {

        String index = variables().generateTemp();
        emitter().emit("for (int %1$s = 0; %1$s < %2$s; %1$s++) {", index, repeat);
        emitter().increaseIndentation();
        emitter().emit("%s$FIFO$ptr[%s$FIFO$index++] = %s[%s];", pname, pname, value, index);
        emitter().decreaseIndentation();
        emitter().emit("}");

    }

}
