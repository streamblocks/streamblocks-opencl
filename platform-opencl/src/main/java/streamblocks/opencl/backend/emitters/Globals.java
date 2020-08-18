package streamblocks.opencl.backend.emitters;

import ch.epfl.vlsc.platformutils.Emitter;
import ch.epfl.vlsc.platformutils.PathUtils;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import streamblocks.opencl.backend.OpenCLBackend;

import java.nio.file.Path;

@Module
public interface Globals {

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();


    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateSource() {
        Path mainTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve("globals.cpp");
        emitter().open(mainTarget);

        emitter().close();
    }

    default void generateHeader() {
        Path mainTarget = PathUtils.getTargetCodeGenInclude(backend().context()).resolve("globals.h");
        emitter().open(mainTarget);

        emitter().close();
    }

}
