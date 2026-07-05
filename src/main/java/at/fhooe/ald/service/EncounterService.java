package at.fhooe.ald.service;

import at.fhooe.ald.dao.CharacterDao;
import at.fhooe.ald.dao.FloorDao;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.Floor;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class EncounterService {
    private final FloorDao floorDao;
    private final CharacterDao characterDao;
    private final DialogueService dialogueService;

    public EncounterService(FloorDao floorDao, CharacterDao characterDao, DialogueService dialogueService) {
        this.floorDao = floorDao;
        this.characterDao = characterDao;
        this.dialogueService = dialogueService;
    }

    public Battle createBattle(int floorNumber) throws SQLException {
        Floor floor = floorDao.findByNumber(floorNumber)
                .orElseThrow(() -> new NoSuchElementException("No floor found for number " + floorNumber));
        return new Battle(
                floor.getFloorNumber(),
                floor.getName(),
                floor.getBackgroundPath(),
                characterDao.findAvailableForFloor(floorNumber),
                floor.getEnemies(),
                dialogueService.getIntroDialogue(floorNumber)
        );
    }
}
