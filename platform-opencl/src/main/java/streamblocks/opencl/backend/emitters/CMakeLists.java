package streamblocks.opencl.backend.emitters;

import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import streamblocks.opencl.backend.OpenCLBackend;

@Module
public interface CMakeLists {

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();
}
