package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import org.multij.Module;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.Expressions;
import streamblocks.opencl.backend.emitters.Statements;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

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

}
