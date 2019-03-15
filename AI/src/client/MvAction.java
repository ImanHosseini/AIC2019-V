package client;

import client.model.Direction;
import client.model.Hero;
import client.model.World;

/**
 * Created by ImanH on 2/13/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class MvAction extends Action {
    Direction dir;
    int curpos,nxtpos;
    public boolean inToOut=false;
    public boolean inToIn=false;
    public MvAction(Hero h,World w,Direction dir,AI ai){
        super(h,w);
        this.price = h.getMoveAPCost();
        this.dir = dir;
        this.isNOP = false;
        this.curpos =ai.fws.toInd(h.getCurrentCell());
        this.nxtpos = ai.fws.nxtInd(curpos,dir);
    }

    public MvAction(Hero h,World w,AI ai){
        super(h,w);
        this.price = 0;
        this.dir = null;
        this.isNOP = true;
        this.curpos =ai.fws.toInd(h.getCurrentCell());
        this.nxtpos = this.curpos;
    }




    public void execute(){
        if(isNOP) return;
        world.moveHero(hero,dir);
    }

    public String toString(){
        String dirString = "NOP";
        if(dir!= null) dirString = dir.toString();
        return "H"+hero.getId()+" pos "+(hero.getCurrentCell().getRow()+"*"+hero.getCurrentCell().getColumn())+" "+dirString+" price: "+price+" val: "+value;
    }
}
