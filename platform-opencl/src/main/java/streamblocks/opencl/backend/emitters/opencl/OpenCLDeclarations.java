package streamblocks.opencl.backend.emitters.opencl;

import org.multij.Module;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.type.StringType;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

@Module
public interface OpenCLDeclarations extends Declarations {

    default TypesEvaluator typeseval(){
        return backend().clTypeseval();
    }

    @Override
    default String declaration(StringType type, String name) {
        throw new UnsupportedOperationException("String type declarartion is not permitted in OpenCL kernels.");
    }

    @Override
    default String portInputDeclaration(String instanceName, PortDecl portDecl, String prefix) {
        throw new UnsupportedOperationException("Not implemented yet !!!");
    }

    @Override
    default String portOutputDeclaration(String instanceName, PortDecl portDecl, String prefix) {
        throw new UnsupportedOperationException("Not implemented yet !!!");
    }

}
