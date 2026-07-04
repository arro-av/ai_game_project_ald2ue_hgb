package at.fhooe.ald.dao;

import at.fhooe.ald.model.DialogueLine;
import java.sql.SQLException;
import java.util.List;

public interface DialogueDao {
    List<DialogueLine> findByFloorNumber(int floorNumber) throws SQLException;
}
