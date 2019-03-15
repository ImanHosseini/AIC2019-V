package client;

import client.model.AbilityName;
import client.model.Cell;
import client.model.Hero;
import client.model.World;

/**
 * Created by ImanH on 2/18/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class HeAction extends Action {
    Cell targetCell;

    public HeAction(Hero h, Cell targetCell, World w, double val){
        super(h,w);
        isNOP= false;
        this.price = hero.getAbility(AbilityName.HEALER_HEAL).getAPCost();
        this.targetCell = targetCell;
        this.value = val;
    }


    public void execute(){
        if(isNOP) return;
        System.out.println("TURN "+world.getCurrentTurn()+" HEAL CALLED!");
        world.castAbility(hero,hero.getAbility(AbilityName.HEALER_HEAL),targetCell);
    }

    public String toString(){
        return "H"+hero.getId()+" pos "+(hero.getCurrentCell().getRow()+"*"+hero.getCurrentCell().getColumn())+" "+"HEAL OPTION"+" price: "+price+" val: "+value;
    }
}
