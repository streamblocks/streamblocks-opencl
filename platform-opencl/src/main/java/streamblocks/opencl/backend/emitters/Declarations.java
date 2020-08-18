package streamblocks.opencl.backend.emitters;

import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.attribute.Types;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.type.BoolType;
import se.lth.cs.tycho.type.ListType;
import se.lth.cs.tycho.type.RefType;
import se.lth.cs.tycho.type.StringType;
import se.lth.cs.tycho.type.Type;
import se.lth.cs.tycho.type.UnitType;

import java.util.stream.Collectors;

@Module
public interface Declarations {

    @Binding(BindingKind.INJECTED)
    Types types();

    @Binding(BindingKind.INJECTED)
    TypesEvaluator typeseval();

    default String declaration(Type type, String name) {
        return typeseval().type(type) + " " + name;
    }

    default String declaration(UnitType type, String name) {
        return "char " + name;
    }

    default String declaration(RefType type, String name) {
        return declaration(type.getType(), String.format("(*%s)", name));
    }

    default String declaration(BoolType type, String name) {
        return "bool " + name;
    }

    default String declaration(StringType type, String name) {
        return "char* " + name;
    }

    default String declarationTemp(Type type, String name) {
        return declaration(type, name);
    }


    /*
     * Declaration for parameters
     */

    default String declarationParameter(Type type, String name) {
        return declaration(type, name);
    }

    default String declarationParameter(RefType type, String name) {
        if (type.getType() instanceof ListType) {
            return declaration(type.getType(), String.format("%s", name));
        } else {
            return declaration(type.getType(), String.format("(*%s)", name));
        }
    }


    default String portDeclaration(PortDecl portDecl){
        Type type = types().declaredPortType(portDecl);
        return String.format("hls::stream< %s> &%s", declaration(type, ""), portDecl.getName());
    }
}
