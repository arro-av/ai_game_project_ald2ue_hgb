package at.fhooe.ald.service;

import at.fhooe.ald.dao.DialogueDao;
import at.fhooe.ald.model.DialogueLine;
import java.sql.SQLException;
import java.util.List;

public class DialogueService {
    private final DialogueDao dialogueDao;

    public DialogueService(DialogueDao dialogueDao) {
        this.dialogueDao = dialogueDao;
    }

    public List<DialogueLine> getIntroDialogue(int floorNumber) throws SQLException {
        return dialogueDao.findByFloorNumber(floorNumber);
    }
}
