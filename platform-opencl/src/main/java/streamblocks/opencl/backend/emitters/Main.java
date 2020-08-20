package streamblocks.opencl.backend.emitters;


import ch.epfl.vlsc.platformutils.Emitter;
import ch.epfl.vlsc.platformutils.PathUtils;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import streamblocks.opencl.backend.OpenCLBackend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Module
public interface Main {
    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateMain() {
        Path mainTarget = PathUtils.getTargetCodeGenSource(backend().context()).resolve("main.cpp");
        emitter().open(mainTarget);

        // -- Flatten Network
        Network network = backend().task().getNetwork();

        // -- Includes
        defineIncludes(network);

        // -- FIFOs
        defineFIFOs(network);

        // -- Ports
        definePorts(network);

        // -- Instantiation
        defineInstancesDeclaration(network);

        // -- Atomic flag of instances
        defineInstanceAtomicRunning(network);

        // -- Main
        defineMain(network);

        emitter().close();
    }

    default void defineIncludes(Network network) {

        backend().includeSystem("stdint.h");
        backend().includeSystem("atomic");
        backend().includeSystem("future");
        emitter().emitNewLine();

        emitter().emit("// -- Instance headers");
        for (Instance instance : network.getInstances()) {
            String headerName = instance.getInstanceName() + ".h";

            backend().includeUser(headerName);
        }
        emitter().emitNewLine();
    }

    default void defineFIFOs(Network network) {
        emitter().emit("// -- FIFOs");
        network.getConnections().forEach(c -> defineFIFO(c));
        emitter().emitNewLine();
    }


    default void defineFIFO(Connection connection) {
        String type = backend().channels().sourceEndTypeSize(connection.getSource());
        int size = backend().channels().connectionBufferSize(connection);
        emitter().emit("FIFO< %s, %s > %s{};",
                type,
                size,
                backend().channels().connectionName(connection));
    }

    default void definePorts(Network network) {
        for (Instance instance : network.getInstances()) {
            GlobalEntityDecl entityDecl = backend().globalnames().entityDecl(instance.getEntityName(), true);
            Entity entity = entityDecl.getEntity();
            for (PortDecl port : entity.getInputPorts()) {
                Connection.End target = new Connection.End(Optional.of(instance.getInstanceName()), port.getName());
                Connection connection = backend().channels().targetEndConnection(target);
                String type = backend().channels().targetEndTypeSize(target);
                int size = backend().channels().targetEndSize(target);
                emitter().emit("Port< %s, %s > %s_%s$FIFO{%s};", type, size, instance.getInstanceName(), port.getName(), backend().channels().connectionName(connection));
            }

            for (PortDecl port : entity.getOutputPorts()) {
                Connection.End source = new Connection.End(Optional.of(instance.getInstanceName()), port.getName());
                String type = backend().channels().sourceEndTypeSize(source);
                int size = backend().channels().sourceEndSize(source);
                List<Connection> targetEndConnections = backend().channels().targetEndConnections(source);

                String fifosConnectedToPort = String.join(", ", targetEndConnections.stream().map(c -> backend().channels().connectionName(c)).collect(Collectors.toList()));

                emitter().emit("Port< %s, %s > %s_%s$FIFO{%s};", type, size, instance.getInstanceName(), port.getName(), fifosConnectedToPort);
            }
        }
        emitter().emitNewLine();
    }

    default void defineInstancesDeclaration(Network network) {
        emitter().emit("// -- Network instance declaration");
        for (Instance instance : network.getInstances()) {
            emitter().emit("c_%s *i_%1$s;", instance.getInstanceName());
        }
        emitter().emitNewLine();
    }

    default void defineInstanceAtomicRunning(Network network) {
        emitter().emit("// -- Instances atomic flags");
        for (Instance instance : network.getInstances()) {
            emitter().emit("std::atomic_flag %s_running;", instance.getInstanceName());
        }
        emitter().emitNewLine();
    }


    default void defineMain(Network network) {
        emitter().emit("int main(int argc, char *argv[]) {");
        {
            emitter().increaseIndentation();
            emitter().emit("// -- Instance instantiation");
            for (Instance instance : network.getInstances()) {
                GlobalEntityDecl entityDecl = backend().globalnames().entityDecl(instance.getEntityName(), true);
                Entity entity = entityDecl.getEntity();

                List<String> io = entity.getInputPorts().stream().map(p -> String.format("%s_%s$FIFO", instance.getInstanceName(), p.getName())).collect(Collectors.toList());
                io.addAll(entity.getOutputPorts().stream().map(p -> String.format("%s_%s$FIFO", instance.getInstanceName(), p.getName())).collect(Collectors.toList()));

                emitter().emit("i_%s = new c_%1$s{%s};", instance.getInstanceName(), String.join(", ", io));
            }
            emitter().emitNewLine();

            emitter().emit("return 0;");
            emitter().decreaseIndentation();
        }
        emitter().emit("}");
    }

}
