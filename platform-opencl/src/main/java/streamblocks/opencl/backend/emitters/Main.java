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

        // -- Command line options
        defineCommandLineOptions();

        // -- Main
        defineMain(network);

        emitter().close();
    }

    default void defineIncludes(Network network) {

        backend().includeSystem("stdint.h");
        backend().includeSystem("atomic");
        backend().includeSystem("future");
        backend().includeUser("Scheduling.h");
        emitter().emitNewLine();

        emitter().emit("// -- Instance headers");
        for (Instance instance : network.getInstances()) {
            String headerName = instance.getInstanceName() + ".h";

            backend().includeUser(headerName);
        }
        emitter().emitNewLine();
        emitter().emit("// -- Header for cmd_L_arguments");
        emitter().emit("#include \"utils.h\"");
        emitter().emitNewLine();
    }

    default void defineCommandLineOptions() {

        emitter().emitNewLine();
        emitter().emit("// -- Add command line options");
        emitter().emitNewLine();
        emitter().emit("cmd_line_options *opt;");
        emitter().emitNewLine();

        emitter().emit("void parse_command_line_input(int argc, char *argv[]) {");
        emitter().increaseIndentation();
        {
            emitter().emit("opt = new cmd_line_options;");
            emitter().emit("std::string name_string = \"\";");
            emitter().emitNewLine();
            emitter().emit("//set default");
            emitter().emit("opt->input_directory = NULL;");
            emitter().emit("opt->input_file = NULL;");
            emitter().emit("opt->output_file = NULL;");
            emitter().emit("opt->output_directory = NULL;");
            emitter().emit("opt->device_name = NULL;");
            emitter().emit("opt->device_type = NULL;");

            emitter().emit("//read command line parameters");
            emitter().emit("for (int i = 1; i < argc; i++) {");
            emitter().increaseIndentation();
            {
                emitter().emit("if (strcmp(argv[i], \"-i\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("opt->input_file = argv[++i];");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-d\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("opt->input_directory = argv[++i];");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-w\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("opt->output_file = argv[++i];");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-h\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("std::cout << \"\\nUsage: [options]\\n\"");
                    emitter().emit("\"\\nCommon arguments:\\n\"");
                    emitter().emit("\"-i <file>     Specify an input file.\\n\"");
                    emitter().emit("\"-h            Print this message.\\n\"");
                    emitter().emit("\"\\nOpenCL specific arguments:\\n\"");
                    emitter().emit("\"-clinfo       Display the names of all the available OpenCL devices.\\n\"");
                    emitter().emit("\"\\nOpenCL Runtime arguments:\\n\"");
                    emitter().emit("\"-dt           Specify device type {CPU or GPU} to be used as accelerator.\\n\"");
                    emitter().emit("\"-dn           Specify device name to be used as accelerator.\\n\"");
                    emitter().emit("\"\\nOther specific arguments:\\n\"");
                    emitter().emit("\"Under construnction.\\n\";");
                    emitter().emit("exit(0);");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-clinfo\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("displayCLInfobyParam();");
                    emitter().emit("exit(0);");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-dt\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("opt->device_type = argv[++i];");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else if (strcmp(argv[i], \"-dn\") == 0) {");
                emitter().increaseIndentation();
                {
                    emitter().emit("opt->device_name = argv[i];");
                    emitter().emit("while (i + 1 < argc) {");
                    emitter().increaseIndentation();
                    {
                        emitter().emit("std::string device_name(argv[++i]);");
                        emitter().emit("if(i + 1 == argc)");
                        emitter().increaseIndentation();
                        {
                            emitter().emit("name_string += device_name;");
                        }
                        emitter().decreaseIndentation();
                        emitter().emit("else name_string += device_name + \" \";");
                    }
                    emitter().decreaseIndentation();
                    emitter().emit("}");

                    emitter().emit("strcpy(opt->device_name, (char*)name_string.c_str());");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");

                emitter().emit("else {");
                emitter().increaseIndentation();
                {
                    emitter().emit("std::cout << \"Error:Unknown input\" << std::endl;");
                    emitter().emit("exit(0);");
                }
                emitter().decreaseIndentation();
                emitter().emit("}");
            }
            emitter().decreaseIndentation();
            emitter().emit("}");
        }
        emitter().decreaseIndentation();
        emitter().emit("}");

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
        emitter().emit("// -- Instance instantiation");
        for (Instance instance : network.getInstances()) {
            GlobalEntityDecl entityDecl = backend().globalnames().entityDecl(instance.getEntityName(), true);
            Entity entity = entityDecl.getEntity();

            List<String> io = entity.getInputPorts().stream().map(p -> String.format("%s_%s$FIFO", instance.getInstanceName(), p.getName())).collect(Collectors.toList());
            io.addAll(entity.getOutputPorts().stream().map(p -> String.format("%s_%s$FIFO", instance.getInstanceName(), p.getName())).collect(Collectors.toList()));

            emitter().emit("auto i_%s = std::make_shared<c_%1$s>(%s);", instance.getInstanceName(), String.join(", ", io));
        }
        emitter().emitNewLine();
    }

    default void vectorOfUniqueInstances(Network network) {
        emitter().emit("// -- Vector of Instances");
        emitter().emit("std::vector<std::shared_ptr<Actor>> instances;");
        for (Instance instance : network.getInstances()) {
            emitter().emit("instances.push_back(i_%s);", instance.getInstanceName());
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

            emitter().emit("parse_command_line_input(argc, argv);");

            // -- FIFOs
            defineFIFOs(network);

            // -- Ports
            definePorts(network);

            // -- Instantiation
            defineInstancesDeclaration(network);

            // -- Vector of unique instances
            vectorOfUniqueInstances(network);

            emitter().emit("// -- RR Scheduling ");
            emitter().emit("scheduling::RR(instances);");
            emitter().emitNewLine();

            emitter().emit("return 0;");
            emitter().decreaseIndentation();
        }
        emitter().emit("}");
    }

}
