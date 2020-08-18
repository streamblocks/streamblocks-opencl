package streamblocks.opencl.backend.emitters;

import ch.epfl.vlsc.platformutils.Emitter;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.ToolValueAttribute;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.type.Type;
import streamblocks.opencl.backend.OpenCLBackend;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Module
public interface Channels {

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default String outputPortTypeSize(Port port) {
        Connection.End source = new Connection.End(Optional.of(backend().instancebox().get().getInstanceName()), port.getName());
        return sourceEndTypeSize(source);
    }

    default String inputPortTypeSize(Port port) {
        return targetEndTypeSize(new Connection.End(Optional.of(backend().instancebox().get().getInstanceName()), port.getName()));
    }

    default String sourceEndTypeSize(Connection.End source) {
        Network network = backend().task().getNetwork();
        List<Connection> connections = network.getConnections().stream()
                .filter(conn -> conn.getSource().equals(source))
                .collect(Collectors.toList());
        Type type = backend().types().connectionType(network, connections.get(0));
        String size = connections.stream()
                .map(c -> sizeToString(connectionBufferSize(c)))
                .collect(Collectors.joining("_"));
        return backend().typeseval().type(type) + "_" + size;
    }

    default String targetEndTypeSize(Connection.End target) {
        Network network = backend().task().getNetwork();
        Connection connection = network.getConnections().stream()
                .filter(conn -> conn.getTarget().equals(target))
                .findFirst().get();
        Type type = backend().types().connectionType(network, connection);
        String size = sizeToString(connectionBufferSize(connection));
        return backend().typeseval().type(type) + "_" + size;
    }

    default String sizeToString(int size) {
        if (size == 0) {
            return "D";
        } else if (size > 0) {
            return Integer.toString(size);
        } else {
            throw new IllegalArgumentException();
        }
    }

    default int connectionBufferSize(Connection connection) {
        Optional<ToolValueAttribute> attribute = connection.getValueAttribute("buffersize");
        if (!attribute.isPresent()) {
            attribute = connection.getValueAttribute("bufferSize");
        }
        if (attribute.isPresent()) {
            return (int) backend().constants().intValue(attribute.get().getValue()).getAsLong();
        } else {
            return 0;
        }
    }

}
