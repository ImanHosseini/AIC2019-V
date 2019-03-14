package client;

import client.model.*;

/**
 * Created by ImanH on 2/13/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public class AtAction extends Action {
    Cell target;
    Ability abil;
    Hero[] targs;
  //  int curpos,nxtpos;
    public AtAction(Hero h, World w, Cell target,Ability abil, AI ai){
        super(h,w);
        this.target = target;
        this.price =abil.getAPCost();
        this.abil = abil;
        this.isNOP = false;
        this.targs = world.getAbilityTargets(AbilityName.BLASTER_BOMB, h.getCurrentCell(), target);
    }

    public AtAction(Hero h, World w, Cell target,Ability abil,Hero[] targs){
        super(h,w);
        this.target = target;
        this.price =abil.getAPCost();
        this.abil = abil;
        this.isNOP = false;
        this.targs= targs;
    }

    public AtAction(Hero h, World w, Cell target,Ability abil){
        super(h,w);
        this.target = target;
        this.price =abil.getAPCost();
        this.abil = abil;
        this.isNOP = false;
    }

    public AtAction(Hero h,World w,AI ai){
        super(h,w);
        this.price = 0;
        this.abil = null;
        this.isNOP = true;

    }

    public AtAction(Hero h,World w){
        super(h,w);
        this.price = 0;
        this.abil = null;
        this.isNOP = true;

    }




    public void execute(){
        if(isNOP) return;
        world.castAbility(hero,abil,target);
    }

    public String toString(){
        return "H"+hero.getId()+" pos "+(hero.getCurrentCell().getRow()+"*"+hero.getCurrentCell().getColumn())+" "+abil.toString()+" target: "+(target.getRow()+"*"+target.getColumn())+ " price: "+price+" val: "+value;
    }
}
