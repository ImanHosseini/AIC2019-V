package client;

import client.model.Hero;
import client.model.World;

/**
 * Created by ImanH on 2/13/2019.
 * Seyed Iman Hosseini Zavaraki
 * Github @ https://github.com/ImanHosseini
 * Wordpress @ https://imanhosseini.wordpress.com/
 */
public abstract class Action {
    World world;
    Hero hero;
    public int price;
    public double value;
    public boolean isNOP;
    public abstract void execute();
    public Action(Hero h,World w){
        this.hero = h;
        this.world = w;
        this.value = 0.0;
    }
}
