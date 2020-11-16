package streamblocks.opencl.backend.emitters.opencl;

import org.multij.Module;
import se.lth.cs.tycho.type.IntType;
import se.lth.cs.tycho.type.ListType;
import se.lth.cs.tycho.type.StringType;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

@Module
public interface OpenCLTypesEvaluator extends TypesEvaluator {

    @Override
    default String type(StringType type) {
        throw new UnsupportedOperationException("String type is not a valid type for OpenCL");
    }

    @Override
    default String type(IntType type) {
        if (type.getSize().isPresent()) {
            int originalSize = type.getSize().getAsInt();
            int targetSize = 8;
            while (originalSize > targetSize) {
                targetSize = targetSize * 2;
            }
            if(type.isSigned()){
                if (targetSize <= 8) {
                    return "char";
                }
                else if (targetSize <= 16) {
                    return "short";
                }
                else if (targetSize <= 32) {
                    return "int";
                }
                else if (targetSize <= 64) {
                    return "long";
                }
                else {
                    return "int";
                }

            }
            else {
                if (targetSize <= 8) {
                    return "unsigned char";
                }
                else if (targetSize <= 16) {
                    return "unsigned short";
                }
                else if (targetSize <= 32) {
                    return "unsigned int";
                }
                else if (targetSize <= 64) {
                    return "unsigned long";
                }
                else {
                    return "unsigned int";
                }
            }
        } else {
            return type.isSigned() ? "int" : "unsigned int";
        }
    }

    @Override
    default String type(ListType type) {
        return String.format("%s", type(type.getElementType()));
    }

}
