package client;

import client.model.AbilityName;
import client.model.Cell;
import client.model.Hero;
import client.model.World;

/**
 * Created by ImanH on 2/16/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class FtAction extends Action {

    Cell targetCell;
    Hero targetHero;

    public FtAction(Hero h, Cell targetCell, Hero targetHero, World w, double val){
        super(h,w);
        this.targetHero = targetHero;
        isNOP= false;
        this.price = h.getAbility(AbilityName.GUARDIAN_DODGE).getAPCost();
        this.targetCell = targetCell;
        this.value = val;
    }




    public void execute(){
        if(isNOP) return;
        world.castAbility(hero,hero.getAbility(AbilityName.GUARDIAN_FORTIFY),targetCell);
    }

    public String toString(){
        return "FTACTION: H"+hero.getId()+" pos "+(hero.getCurrentCell().getRow()+"*"+hero.getCurrentCell().getColumn())+" "+"FORTIFY OPTION"+" price: "+price+" val: "+value;
    }
}
