package streamblocks.opencl.backend.emitters;

import ch.epfl.vlsc.platformutils.Emitter;
import ch.epfl.vlsc.settings.PlatformSettings;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.attribute.ScopeLiveness;
import se.lth.cs.tycho.ir.Annotation;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.Transition;
import se.lth.cs.tycho.ir.entity.am.ctrl.Exec;
import se.lth.cs.tycho.ir.entity.am.ctrl.Instruction;
import se.lth.cs.tycho.ir.entity.am.ctrl.InstructionKind;
import se.lth.cs.tycho.ir.entity.am.ctrl.State;
import se.lth.cs.tycho.ir.entity.am.ctrl.Test;
import se.lth.cs.tycho.ir.entity.am.ctrl.Wait;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.OnOffSetting;
import streamblocks.opencl.backend.OpenCLBackend;

import javax.lang.model.type.NullType;
import java.util.*;
import java.util.function.Function;

@Module
public interface Controllers {
    String ACC_ANNOTATION = "acc";

    @Binding(BindingKind.INJECTED)
    OpenCLBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }


    default void emitController(String name, ActorMachine actorMachine) {
        List<? extends State> stateList = actorMachine.controller().getStateList();
        Map<State, Integer> stateMap = stateMap(stateList);
        Set<State> waitTargets = collectWaitTargets(stateList);

        emitter().emit("bool c_%s::schedule() {", name);
        emitter().increaseIndentation();

        emitter().emit("bool progress = false;");
        emitter().emit("");

        // -- Check and add OpenCL port buffers functions
        List<String> clTextList = new ArrayList<String>();
        String clText = "";
        for (int i = 0; i < actorMachine.getTransitions().size(); i++) {
            Transition transition = actorMachine.getTransitions().get(i);
            int index = actorMachine.getTransitions().indexOf(transition);
            if (Annotation.hasAnnotationWithName(ACC_ANNOTATION, transition.getAnnotations())) {
                for (Map.Entry<Port, Integer> entry : transition.getInputRates().entrySet()) {
                    clText = entry.getKey().getName() + "$FIFO" + ".opencl_read_done();";
                    if(!clTextList.contains(clText)){
                        clTextList.add(clText);
                    }
                }
                for (Map.Entry<Port, Integer> entry : transition.getOutputRates().entrySet()) {
                    clText = entry.getKey().getName() + "$FIFO" + ".opencl_write_done(this->ocl);";
                    if(!clTextList.contains(clText)){
                        clTextList.add(clText);
                    }
                }
            }
        }
        for(int i = 0; i < clTextList.size(); i++){
            emitter().emitRawLine("\t" + clTextList.get(i));
        }
        emitter().emitNewLine();
        /*if (backend().clInstance().anyAccTransition(actorMachine)) {
            emitter().emit("// -- Check OpenCL port buffers status");
            if (!actorMachine.getInputPorts().isEmpty()) {
                for (PortDecl port : actorMachine.getInputPorts()) {
                    String portString = port.getName() + "$FIFO";
                    emitter().emit("%s.opencl_read_done();", portString);
                }
            }
            if (!actorMachine.getOutputPorts().isEmpty()) {
                for (PortDecl port : actorMachine.getOutputPorts()) {
                    String portString = port.getName() + "$FIFO";
                    emitter().emit("%s.opencl_write_done(ocl);", portString);
                }
            }
            emitter().emitNewLine();
        }*/

        jumpInto(waitTargets.stream().mapToInt(stateMap::get).collect(BitSet::new, BitSet::set, BitSet::or), stateMap.get(actorMachine.controller().getInitialState()));

        Function<Instruction, BitSet> initialize;
        if (backend().context().getConfiguration().get( PlatformSettings.scopeLivenessAnalysis)) {
            ScopeLiveness liveness = new ScopeLiveness(backend().scopes(), actorMachine, backend().scopeDependencies());
            initialize = liveness::init;
        } else {
            initialize = instruction -> backend().scopes().init(actorMachine, instruction);
        }

        for (State s : stateList) {
            emitter().emit("S%d:", stateMap.get(s));
            Instruction instruction = s.getInstructions().get(0);
            initialize.apply(instruction).stream().forEach(scope ->
                    emitter().emit("scope_%d();", scope)
            );
            emitInstruction(name, instruction, stateMap);
        }

        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default Map<State, Integer> stateMap(List<? extends State> stateList) {
        int i = 0;
        Map<State, Integer> result = new HashMap<>();
        for (State s : stateList) {
            result.put(s, i++);
        }
        return result;
    }

    void emitInstruction(String name, Instruction instruction, Map<State, Integer> stateNumbers);

    default void emitInstruction(String name, Test test, Map<State, Integer> stateNumbers) {
        emitter().emit("if (condition_%d()) {", test.condition());
        emitter().increaseIndentation();
        emitter().emit("goto S%d;", stateNumbers.get(test.targetTrue()));
        emitter().decreaseIndentation();
        emitter().emit("} else {");
        emitter().increaseIndentation();
        emitter().emit("goto S%d;", stateNumbers.get(test.targetFalse()));
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void emitInstruction(String name, Wait wait, Map<State, Integer> stateNumbers) {
        emitter().emit("this->program_counter = %d;", stateNumbers.get(wait.target()));
        emitter().emit("return progress;");
        emitter().emit("");
    }

    default void emitInstruction(String name, Exec exec, Map<State, Integer> stateNumbers) {
        emitter().emit("transition_%d();", exec.transition());
        emitter().emit("progress = true;");
        emitter().emit("goto S%d;", stateNumbers.get(exec.target()));
        emitter().emit("");
    }

    default void jumpInto(BitSet waitTargets, int initialState) {
        emitter().emit("switch (this->program_counter) {");
        waitTargets.stream().forEach(s -> emitter().emit("case %d: goto S%1$d;", s));
        emitter().emit("default: goto S%d;", initialState);
        emitter().emit("}");
        emitter().emit("");
    }

    default Set<State> collectWaitTargets(List<? extends State> stateList) {
        Set<State> targets = new HashSet<>();
        for (State state : stateList) {
            Instruction i = state.getInstructions().get(0);
            if (i.getKind() == InstructionKind.WAIT) {
                i.forEachTarget(targets::add);
            }
        }
        return targets;
    }

}

