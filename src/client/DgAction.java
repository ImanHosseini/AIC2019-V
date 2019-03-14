package client;

import client.model.Cell;
import client.model.Direction;
import client.model.Hero;
import client.model.World;

/**
 * Created by ImanH on 2/16/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class DgAction extends Action {

    Cell targetCell;

    public DgAction(Hero h,Cell targetCell,World w,double val){
        super(h,w);
        isNOP= false;
        this.price = h.getDodgeAbilities()[0].getAPCost();
        this.targetCell = targetCell;
        this.value = val;
    }




    public void execute(){
        if(isNOP) return;
        world.castAbility(hero,hero.getDodgeAbilities()[0],targetCell);
    }

    public String toString(){
        return "H"+hero.getId()+" pos "+(hero.getCurrentCell().getRow()+"*"+hero.getCurrentCell().getColumn())+" "+"DODGE OPTION"+" price: "+price+" val: "+value;
    }
}
