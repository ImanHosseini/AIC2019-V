package client;

import client.model.Cell;
import client.model.Hero;
import client.model.Map;
import client.model.Ability;
import client.model.AbilityType;


import java.util.ArrayList;
import java.util.List;


public class AbilityTools {
    private Map map;
    private VisionTools visionTools;
    private List<Hero> myHeroes;
    private List<Hero> oppHeroes;

    public AbilityTools(Map map, List<Hero> myHeroes, List<Hero> oppHeroes) {
        this.map = map;
        visionTools = new VisionTools(map);
        this.myHeroes = myHeroes;
        this.oppHeroes = oppHeroes;
    }

    /*public Hero[] getAbilityTargets(Ability ability, Cell startCell, Cell targetCell) {
        Cell[] impactCells = getImpactCells(ability, startCell, targetCell);
        Set<Cell> affectedCells = new HashSet<>();
        for (Cell cell : impactCells) {
            affectedCells.addAll(getCellsInAOE(cell, ability.getAreaOfEffect()));
        }
        if (ability.getType() == AbilityType.DEFENSIVE) {
            return getMyHeroesInCells(affectedCells.toArray(new Cell[0]));
        } else {
            return getOppHeroesInCells(affectedCells.toArray(new Cell[0]));
        }
    }*/

    public Hero[] getENEMYAbilityTargets(Ability ability, Cell startCell, Cell targetCell) {
        Cell[] impactCells = getImpactCells(ability, startCell, targetCell);
        List<Cell> affectedCells = getCellsInAOE(impactCells[impactCells.length - 1],
                ability.getAreaOfEffect());
            return getMyHeroesInCells(affectedCells.toArray(new Cell[0]));

    }

    public Cell getImpactCell(Ability ability, Cell startCell, Cell targetCell) {
        Cell[] impactCells = getImpactCells(ability, startCell, targetCell);
        return impactCells[impactCells.length - 1];
    }

    public Cell[] getImpactCells(Ability ability, Cell startCell, Cell targetCell) {
        if ((!ability.isLobbing() && startCell.isWall()) || startCell == targetCell) {
            return new Cell[]{startCell};
        }
        List<Cell> impactCells = new ArrayList<>();
        Cell[] rayCells = visionTools.getRayCells(startCell, targetCell, ability.isLobbing());
        Cell lastCell = null;
        for (Cell cell : rayCells) {
            if (visionTools.manhattanDistance(startCell, cell) > ability.getRange())
                break;
            lastCell = cell;
            if ((getOppHero(cell) != null && !ability.getType().equals(AbilityType.DEFENSIVE))
                    || (getMyHero(cell) != null && ability.getType().equals(AbilityType.DEFENSIVE))) {
                impactCells.add(cell);
                if (!ability.isLobbing()) break;
            }
        }
        if (!impactCells.contains(lastCell))
            impactCells.add(lastCell);
        return impactCells.toArray(new Cell[0]);
    }

    public Hero getOppHero(Cell cell) {
        for (Hero hero : oppHeroes) {
            if (hero.getCurrentCell() == cell)
                return hero;
        }
        return null;
    }

    public Hero getOppHero(int cellRow, int cellColumn) {
        if (!map.isInMap(cellRow, cellColumn)) return null;
        return getOppHero(map.getCell(cellRow, cellColumn));
    }

    public Hero getMyHero(Cell cell) {
        for (Hero hero : myHeroes) {
            if (hero.getCurrentCell() == cell)
                return hero;
        }
        return null;
    }

    private List<Cell> getCellsInAOE(Cell impactCell, int AOE) {
        List<Cell> cells = new ArrayList<>();
        for (int row = impactCell.getRow() - AOE; row <= impactCell.getRow() + AOE; row++) {
            for (int col = impactCell.getColumn() - AOE; col <= impactCell.getColumn() + AOE; col++) {
                if (!map.isInMap(row, col)) continue;
                Cell cell = map.getCell(row, col);
                if (visionTools.manhattanDistance(impactCell, cell) <= AOE)
                    cells.add(cell);
            }
        }
        return cells;
    }

    private Hero[] getOppHeroesInCells(Cell[] cells) {
        List<Hero> heroes = new ArrayList<>();
        for (Cell cell : cells) {
            Hero hero = getOppHero(cell);
            if (hero != null) {
                heroes.add(hero);
            }
        }
        return heroes.toArray(new Hero[0]);
    }

    private Hero[] getMyHeroesInCells(Cell[] cells) {
        List<Hero> heroes = new ArrayList<>();
        for (Cell cell : cells) {
            Hero hero = getMyHero(cell);
            if (hero != null) {
                heroes.add(hero);
            }
        }
        return heroes.toArray(new Hero[0]);
    }
}
