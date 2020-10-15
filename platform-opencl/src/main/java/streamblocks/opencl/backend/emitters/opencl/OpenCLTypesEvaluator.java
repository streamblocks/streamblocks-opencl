package streamblocks.opencl.backend.emitters.opencl;

import org.multij.Module;
import se.lth.cs.tycho.type.StringType;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

@Module
public interface OpenCLTypesEvaluator extends TypesEvaluator {

    @Override
    default String type(StringType type) {
        throw new UnsupportedOperationException("String type is not a valid type for OpenCL");
    }

}
