package at.fhooe.ald.model.fsm;

import java.util.Optional;

public interface StateMachine<S extends Enum<S>> {
    S getCurrentState();

    Optional<String> update();
}
