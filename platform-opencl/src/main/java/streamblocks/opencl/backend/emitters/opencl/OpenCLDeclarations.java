package streamblocks.opencl.backend.emitters.opencl;

import org.multij.Module;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.type.ListType;
import se.lth.cs.tycho.type.StringType;
import se.lth.cs.tycho.type.Type;
import streamblocks.opencl.backend.emitters.Declarations;
import streamblocks.opencl.backend.emitters.TypesEvaluator;

@Module
public interface OpenCLDeclarations extends Declarations {

    default TypesEvaluator typeseval(){
        return backend().clTypeseval();
    }

    @Override
    default String declaration(StringType type, String name) {
        return typeseval().type(type) + " " + name;
    }

    @Override
    default String declaration(Type type, String name) {
        return typeseval().type(type) + " " + name;
    }

    @Override
    default String declaration(ListType type, String name) {
        return String.format("%s %s%s", typeseval().type(type), name, getListDims(type));
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
