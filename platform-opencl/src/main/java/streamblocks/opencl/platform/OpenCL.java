package streamblocks.opencl.platform;

import se.lth.cs.tycho.compiler.Compiler;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.phase.Phase;
import se.lth.cs.tycho.phase.RemoveUnusedEntityDeclsPhase;
import se.lth.cs.tycho.platform.Platform;
import streamblocks.opencl.phase.OpenCLBackendPhase;

import java.util.List;

public class OpenCL implements Platform {
    @Override
    public String name() {
        return "opencl";
    }

    @Override
    public String description() {
        return "StreamBlocks OpenCL Platform";
    }

    private static final List<Phase> phases = ImmutableList.<Phase>builder()
            .addAll(Compiler.frontendPhases())
            .addAll(Compiler.templatePhases())
            .addAll(Compiler.networkElaborationPhases())
            .addAll(Compiler.nameAndTypeAnalysis())
            .addAll(Compiler.actorMachinePhases())
            .add(new RemoveUnusedEntityDeclsPhase())
            .add(new OpenCLBackendPhase())
            .build();


    @Override
    public List<Phase> phases() {
        return phases;
    }
}
