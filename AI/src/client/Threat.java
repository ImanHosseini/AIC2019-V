package client;

import client.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ImanH on 2/16/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class Threat {
    AbilityName ability;
    public Hero attacker;
    public Hero[] tHeros;
    public Cell targetCell;
    public double pwr;
    public double multiplicity;
    AbilityTools at;
    public Threat(Hero attacker, Hero[] tHeros, int pwr, Cell targetCell, AbilityName ability,AbilityTools at){
        this.attacker = attacker;
        this.tHeros = tHeros;
        this.pwr =(double) pwr;
        this.targetCell = targetCell;
        this.ability = ability;
        this.multiplicity = 1.0;
        this.at = at;
    }

    public Threat(Hero attacker, Hero[] tHeros, int pwr, Cell targetCell, AbilityName ability){
        this.attacker = attacker;
        this.tHeros = tHeros;
        this.pwr =(double) pwr;
        this.targetCell = targetCell;
        this.ability = ability;
        this.multiplicity = 1.0;
        this.at =null;
    }

    public Threat(Hero attacker, Hero tHero, int pwr, Cell targetCell, AbilityName ability){
        this.attacker = attacker;
        this.tHeros = new Hero[]{tHero};
        this.pwr =(double) pwr;
        this.targetCell = targetCell;
        this.ability = ability;
        this.multiplicity = 1.0;
        this.at =null;
    }




    public boolean isAffected(Cell cell){
        switch (ability){
            case GUARDIAN_ATTACK:{
                return (dist(cell,targetCell)<2);
            }
            case SENTRY_ATTACK:{
                Cell[] celz = at.getImpactCells(attacker.getAbility(AbilityName.SENTRY_ATTACK),attacker.getCurrentCell(),targetCell);
                Set<Cell> mySet = new HashSet<Cell>(Arrays.asList(celz));
                return mySet.contains(cell);
            }
            case SENTRY_RAY:{
                Cell[] celz = at.getImpactCells(attacker.getAbility(AbilityName.SENTRY_RAY),attacker.getCurrentCell(),targetCell);
                Set<Cell> mySet = new HashSet<Cell>(Arrays.asList(celz));
                return mySet.contains(cell);
            }
            case HEALER_ATTACK:{
                return (cell.getColumn()==targetCell.getColumn() && cell.getRow() == targetCell.getRow());
            }
            case BLASTER_ATTACK:{
                Cell[] celz = at.getImpactCells(attacker.getAbility(AbilityName.BLASTER_ATTACK),attacker.getCurrentCell(),targetCell);
                Set<Cell> mySet = new HashSet<Cell>(Arrays.asList(celz));
                return mySet.contains(cell);
            }
            case BLASTER_BOMB:{
                Cell[] celz = at.getImpactCells(attacker.getAbility(AbilityName.BLASTER_BOMB),attacker.getCurrentCell(),targetCell);
                Set<Cell> mySet = new HashSet<Cell>(Arrays.asList(celz));
                return mySet.contains(cell);
            }
        }
        return false;
    }

    private int dist(Cell c1,Cell c2){
        return Math.abs(c1.getRow()-c2.getRow())+Math.abs(c1.getColumn()-c2.getColumn());
    }
}
