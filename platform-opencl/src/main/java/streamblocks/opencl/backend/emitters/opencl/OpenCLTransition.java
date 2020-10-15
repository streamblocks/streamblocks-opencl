package streamblocks.opencl.backend.emitters.opencl;

import ch.epfl.vlsc.platformutils.Emitter;
import ch.epfl.vlsc.platformutils.PathUtils;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.ir.entity.am.Transition;
import streamblocks.opencl.backend.OpenCLBackend;

import java.nio.file.Path;

@Module
public interface OpenCLTransition {

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();


    default Emitter emitter() {
        return backend().clEmitter();
    }

    default OpenCLStatements statements() {
        return backend().clStatements();
    }


    default void acceleratedTransition(String instanceName, Transition transition, int index) {
        String name = instanceName + "_transition_" + index;

        Path instanceTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve(name + ".cl");
        emitter().open(instanceTarget);


        emitter().emit("__kernel void %s() {", name);
        {
            emitter().increaseIndentation();

            transition.getBody().forEach(statements()::execute);

            emitter().decreaseIndentation();
        }

        emitter().emit("}");
        emitter().close();
    }
}
