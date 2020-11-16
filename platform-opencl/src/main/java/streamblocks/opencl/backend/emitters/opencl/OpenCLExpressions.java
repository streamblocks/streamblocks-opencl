package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import org.multij.Module;
import se.lth.cs.tycho.ir.decl.GeneratorVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.*;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.type.AlgebraicType;
import se.lth.cs.tycho.type.ListType;
import se.lth.cs.tycho.type.Type;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.Expressions;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Module
public interface OpenCLExpressions extends Expressions {

    @Override
    default Emitter emitter() {
        return backend().clEmitter();
    }


    @Override
    default Declarations declarations(){
        return backend().clDeclarations();
    }

    default OpenCLStatements statements() {
        return backend().clStatements();
    }

    @Override
    default TypesEvaluator typeseval() {
        return backend().clTypeseval();
    }

    @Override
    default String evaluate(ExprInput input) {
        String tmp = variables().generateTemp();
        Type type = types().type(input);
        if (input.hasRepeat()) {
            if (input.getOffset() == 0) {
                return input.getPort().getName() + "$FIFO$ptr[" + input.getPort().getName() + "$FIFO$index++]";
                //emitter().emit("%s$FIFO.elements_preview(%s, %d);", input.getPort().getName(), tmp, input.getRepeat());
            } else {
                throw new RuntimeException("not implemented");
            }
        } else {
            emitter().emit("%s = %s;", declarations().declaration(type, tmp), backend().defaultValues().defaultValue(type));
            if (input.getOffset() == 0) {
                emitter().emit("%s = %s$FIFO$ptr[%s$FIFO$index++];", tmp, input.getPort().getName(), input.getPort().getName());
            } else {
                emitter().emit("%s = %s$FIFO.element_preview(%s);", tmp, input.getPort().getName(), input.getOffset());
            }
        }
        return tmp;
    }


    @Override
    default String evaluate(ExprList list) {
        ListType t = (ListType) types().type(list);
        if (t.getSize().isPresent()) {
            Type elementType = t.getElementType();
            String name = variables().generateTemp();
            String decl = declarations().declaration(t, name);
            String value = list.getElements().stream().sequential()
                    .map(element -> {
                        if (elementType instanceof AlgebraicType || backend().alias().isAlgebraicType(elementType)) {
                            String tmp = variables().generateTemp();
                            emitter().emit("%s = %s;", backend().declarations().declaration(elementType, tmp), backend().defaultValues().defaultValue(elementType));
                            backend().statements().copy(elementType, tmp, elementType, evaluate(element));
                            return tmp;
                        }
                        return evaluate(element);
                    })
                    .collect(Collectors.joining(", ", " {", "}"));
            value = value.replaceAll("this->", "");
            decl = decl.replace(name, "$$");
            //emitter().emit("%s = %s;", decl, value);
            String s = decl + " = " + value;
            return s;
        } else {
            return "NULL /* TODO: implement dynamically sized lists */";
        }
    }

    @Override
    default String evaluate(ExprIf expr) {
        Type type = types().type(expr);
        String temp = variables().generateTemp();
        String decl = declarations().declaration(type, temp);
        emitter().emit("%s = %s;", decl, backend().defaultValues().defaultValue(type));
        emitter().emit("if (%s) {", evaluate(expr.getCondition()));
        emitter().increaseIndentation();
        Type thenType = types().type(expr.getThenExpr());
        String thenValue = evaluate(expr.getThenExpr());
        backend().clStatements().copy(type, temp, thenType, thenValue);
        emitter().decreaseIndentation();
        emitter().emit("} else {");
        emitter().increaseIndentation();
        Type elseType = types().type(expr.getElseExpr());
        String elseValue = evaluate(expr.getElseExpr());
        backend().clStatements().copy(type, temp, elseType, elseValue);
        emitter().decreaseIndentation();
        emitter().emit("}");
        return temp;
    }

    @Override
    default String evaluate(ExprLet let) {
        //let.forEachChild(backend().callables()::declareEnvironmentForCallablesInScope);
        for (VarDecl decl : let.getVarDecls()) {
            Type type = types().declaredType(decl);
            String name = variables().declarationName(decl);
            emitter().emit("%s = %s;", declarations().declaration(type, name), backend().defaultValues().defaultValue(type));
            statements().copy(type, name, types().type(decl.getValue()), evaluate(decl.getValue()));
        }
        return evaluate(let.getBody());
    }

    @Override
    default String evaluateComprehension(ExprComprehension comprehension, ListType t) {
        String name = variables().generateTemp();
        String decl = declarations().declaration(t, name);
        emitter().emit("%s = %s;", decl, backend().defaultValues().defaultValue(t));
        String index = variables().generateTemp();
        emitter().emit("int %s = 0;", index);
        evaluateListComprehension(comprehension, name, index);
        return name;
    }

    @Override
    default String exprIndexing(ListType type, ExprIndexer indexer) {
        String s = evaluate(indexer.getStructure());
        s = s.replace("this->", "");
        return String.format("%s[%s]", s, evaluate(indexer.getIndex()));
    }

    @Override
    default void withGenerator(ExprBinaryOp binOp, ImmutableList<GeneratorVarDecl> varDecls, Runnable action) {
        if (binOp.getOperations().equals(Collections.singletonList(".."))) {
            String from = evaluate(binOp.getOperands().get(0));
            String to = evaluate(binOp.getOperands().get(1));
            for (VarDecl d : varDecls) {
                Type type = types().declaredType(d);
                String name = variables().declarationName(d);
                emitter().emit("%s = %s;", declarations().declaration(type, name), from);
                emitter().emit("while (%s <= %s) {", name, to);
                emitter().increaseIndentation();
            }
            action.run();
            List<VarDecl> reversed = new ArrayList<>(varDecls);
            Collections.reverse(reversed);
            for (VarDecl d : reversed) {
                emitter().emit("%s++;", variables().declarationName(d));
                emitter().decreaseIndentation();
                emitter().emit("}");
            }
        } else {
            throw new UnsupportedOperationException(binOp.getOperations().get(0));
        }
    }
}
