package client;

import client.model.*;
import client.model.Map;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AI {
    int dist_to_fray = 0;
    ArrayList<dodge_stub> dodge_queue = new ArrayList<>();
    boolean[] dodger = new boolean[4];
    HashSet<Integer> dodging_pts = new HashSet<>();
    boolean standApart = true;
    VisionTools global_vs;
    int eDead = 0;
    int latestAp;
    int mvmnt_budget;
    int last_fighters;
    public boolean is4G = true;
    int heros_in_fray;
    int enemies_incoming;
    int enemies_in;

    boolean lookAhead = true;
    String conf;
    int Gleft, Rleft, Sleft, Hleft, Zleft;
    boolean haveGuard = false;
    boolean haveHealer = false;
    int healer_id = -1;
    ArrayList<Integer> wounded_inds = new ArrayList<>();
    public static final boolean DBG_MVM = false;
    public static final boolean DBG_ATK = false;
    public static final boolean DBG_FT = false;
    World world;
    public boolean[][] rayable;
    FloydWarshallSolver fws;
    int col_n, row_n;
    int[] obj_zone_indices;
    ArrayList<Integer> fray_inds = new ArrayList<>();
    int[] myrs_indices;
    int[] opprs_indices;
    int[] enemy_inds = new int[4];
    boolean noEnemeyInd = true;
    int[] my_inds = new int[4];
    boolean noMyInd = true;
    HashMap<Hero, Integer> my_hero_to_ind;
    HashMap<Hero, Integer> opp_hero_to_ind;
    HashMap<Integer, OpHeroInf> oppHeros;

    /* MAP LAYOUT ===========================
    0,0 - 0,1 - 0,2 - ... - 0,col_n-1
    1,0 - ..
    2,0 - ..
    ..
    row_n-1,0 -..         - row_n-1,col_n-1
    =========================================
    0 , 1 , 2 , ... , col_n-1
    col_n , col_n+1, ...
    2*col_n , ...
    =========================================
    (i, j) => i*col_n + j
    N => (N/col_n, N % col_n)
     */


    public AI(String conf) {
        this.conf = conf;
        this.Gleft = Integer.parseInt(Character.toString(conf.charAt(1)));
        this.Sleft = Integer.parseInt(Character.toString(conf.charAt(3)));
        this.Rleft = Integer.parseInt(Character.toString(conf.charAt(5)));
        this.Hleft = Integer.parseInt(Character.toString(conf.charAt(7)));
//        this.Gleft = 0;
//        this.Rleft=2;
//        this.Sleft=2;
        if (Gleft > 0) haveGuard = true;
        if(Hleft>0) haveHealer = true;
    }


    public AI() {
        this.conf = "";
        this.Gleft = 0;
        this.Rleft = 4;
        this.Sleft = 0;
        this.Hleft = 0;
        this.Zleft=0;
        if (Gleft > 0) haveGuard = true;
        //  if(Hleft>0) haveHealer = true;
    }

    public void preProcess(World world) {
        this.last_fighters = 0;
        this.oppHeros = new HashMap<>();
        this.world = world;

       // setRayable(world.getMap());
        col_n = world.getMap().getColumnNum();
        row_n = world.getMap().getRowNum();
        fws = new FloydWarshallSolver(world.getMap());
        fws.solve();
        // System.out.println("SOLVED! "+fws.solved);
        set_ind_arrays(world);
        dist_to_fray = (int)fws.setDist(obj_zone_indices,myrs_indices);
    }

    public void pickTurn(World world) {
        this.world = world;
        // System.out.println("pick started");
        if (Gleft > 0) {
            world.pickHero(HeroName.GUARDIAN);
            Gleft--;
            return;
        }
        if (Sleft > 0) {
            world.pickHero(HeroName.SENTRY);
            Sleft--;
            return;
        }
        if (Rleft > 0) {
            world.pickHero(HeroName.BLASTER);
            Rleft--;
            return;
        }
        if (Hleft > 0) {
            world.pickHero(HeroName.HEALER);
            Hleft--;
            return;
        }
        if(Zleft>0){
            world.pickHero(HeroName.SHADOW);
            Zleft--;
            return;
        }
        // System.out.println("picked a guard at "+world.getCurrentTurn());
    }

    public void moveTurn(World world) {


        // long t0 = System.currentTimeMillis();

        int j = 0;
        int j2 = 0;
        wounded_inds.clear();
        for (int i = 0; i < 4; i++) {
            if (world.getOppHeroes()[i].getCurrentCell().getRow() == -1) {
            } else {
                enemy_inds[j] = fws.toInd(world.getOppHeroes()[i].getCurrentCell());
                j++;
                noEnemeyInd = false;
            }
            if (world.getMyHeroes()[i].getCurrentHP() == 0) {
            } else {
                if (world.getMyHeroes()[i].getCurrentHP() < 80)
                    wounded_inds.add(fws.toInd(world.getMyHeroes()[i].getCurrentCell()));
                my_inds[j2] = fws.toInd(world.getMyHeroes()[i].getCurrentCell());
                j2++;
                noMyInd = false;
            }
        }
        if (!noMyInd) {
            for (int i = j; j < 4; j++) {
                my_inds[j] = enemy_inds[i];
            }
        }
        if (!noEnemeyInd) {
            for (int i = j; j < 4; j++) {
                enemy_inds[j] = enemy_inds[i];
            }
        }
        this.world = world;
        int deads = world.getOppDeadHeroes().length;
        int delta_deads = deads - eDead;
        if (delta_deads < 0) delta_deads = 0;
        int deltaAP = 0;
        fray_inds.clear();
        if (world.getMovePhaseNum() == 0) {
            heros_in_fray = 0;
            enemies_in = 0;
            enemies_incoming = 0;
            latestAp = world.getAP();

            for (Hero h : world.getMyHeroes()) {
                if (h.getCurrentCell().isInObjectiveZone()) {
                    heros_in_fray++;
                }
            }
            for (Hero h : world.getOppHeroes()) {
                if (h.getCurrentCell().getRow() == -1) continue;
                if (fws.getDistClosestInSet(fws.toInd(h.getCurrentCell()), obj_zone_indices) == 0) enemies_in++;
                else if (fws.getDistClosestInSet(fws.toInd(h.getCurrentCell()), obj_zone_indices) < 4)
                    enemies_incoming++;
            }
            mvmnt_budget = Math.max(world.getAP() - (enemies_in) * 25, 20);
        } else {
            heros_in_fray = 0;
            enemies_in = 0;
            enemies_incoming = 0;
            latestAp = world.getAP();

            for (Hero h : world.getMyHeroes()) {
                if (h.getCurrentCell().isInObjectiveZone()) heros_in_fray++;
            }
            for (Hero h : world.getOppHeroes()) {
                if (h.getCurrentCell().getRow() == -1) continue;
                if (fws.getDistClosestInSet(fws.toInd(h.getCurrentCell()), obj_zone_indices) == 0) enemies_in++;
                else if (fws.getDistClosestInSet(fws.toInd(h.getCurrentCell()), obj_zone_indices) < 4)
                    enemies_incoming++;
            }
            int newAP = world.getAP();
            deltaAP = newAP - latestAp;
            mvmnt_budget -= deltaAP;
        }

        if (noEnemeyInd || fws.setDist(my_inds, enemy_inds) > 10) mvmnt_budget = world.getAP();

        if (world.getMovePhaseNum() > 3 && !noMyInd) {
            if (noEnemeyInd || fws.setDist(my_inds, enemy_inds) > 8) mvmnt_budget = world.getAP();
            if (!noEnemeyInd && fws.setDist(my_inds, enemy_inds) > 7) mvmnt_budget += 20;
            else if (!noEnemeyInd && fws.setDist(my_inds, enemy_inds) > 6) mvmnt_budget += 12;
        }

        // set-up hacts
        if (world.getMovePhaseNum() == 0) {
            dodger = new boolean[4];
            dodge_queue = new ArrayList<>();
            dodging_pts = new HashSet<>();
        }
        HeroActs[] hacts = new HeroActs[4];
        int i = 0;
        for (Hero h : world.getMyHeroes()) {
            HeroActs ha = new HeroActs(h);
            ha.index = i;
            setUpMvActs(ha);
            hacts[i] = ha;
            i++;
        }

        // decide
        decide(hacts, Math.min(mvmnt_budget, world.getAP()));

    }

    public void actionTurn(World world) {
        // System.out.println("ME: "+world.getMyScore()+"** OPP: "+world.getOppScore());
        // long t0 = System.currentTimeMillis();
        this.world = world;
        global_vs = new VisionTools(world.getMap());


        opp_hero_to_ind = new HashMap<>();
        for(int i=0;i<4;i++){
            Hero eh = world.getOppHeroes()[i];
            opp_hero_to_ind.put(eh,i);
        }

        // Update info
        updateInfo();
        // set up attack options
        // set-up hacts
        int i = 0;
        HeroActs[] hacts = new HeroActs[4];
        for (i = 0; i < 4; i++) {
            Hero h = world.getMyHeroes()[i];
            HeroActs ha = new HeroActs(h);
            ha.index = i;
            if (dodger[i]) {
                ha.acts = new ArrayList<>();
                hacts[i] = ha;
                continue;
            }
            setUpAcActs(ha);
            hacts[i] = ha;
        }
        for (dodge_stub ds : dodge_queue) {
            // System.out.println("DOGE: "+ds.h_ind+"TO "+ds.tind);
            hacts[ds.h_ind].acts.add(ds.makeDgAction());
        }
        // analyze threats
        HeroThreats[] hthreats = new HeroThreats[4];
        HeroThreats[] myhts = new HeroThreats[4];
        my_hero_to_ind = new HashMap<>();
        opp_hero_to_ind = new HashMap<>();
        for (i = 0; i < 4; i++) {
            myhts[i] = new HeroThreats(world.getMyHeroes()[i]);
            my_hero_to_ind.put(world.getMyHeroes()[i], i);
        }
        for (i = 0; i < 4; i++) {
            my_hero_to_ind.put(world.getOppHeroes()[i], i);
        }
        for (int j = 0; j < 4; j++) {
            Hero h = world.getOppHeroes()[j];
            HeroThreats ht = new HeroThreats(h);
            setUpHts(ht, myhts);
            hthreats[j] = ht;
        }
        for (int k = 0; k < 4; k++) {
            for (Threat thr : hthreats[k].threats) {
                thr.multiplicity = (double) hthreats[k].threats.size();
            }
        }
        for (int k = 0; k < 4; k++) {
            for (Threat thr : myhts[k].threats) {
                myhts[k].max_dmg += thr.pwr / thr.multiplicity;
                myhts[k].noMul_max_dmg += thr.pwr;
            }
        }
        // make dodge options
        // FOR NOW: only take best DODGE => what if we dodge-collide?
        // TODO: rate dodge locations [better]
        for (i = 0; i < 4; i++) {
            if (dodger[i]) continue;
            HashMap<Cell, Double> dcellz = new HashMap<>();
            Hero h = hacts[i].h;
            int col = h.getCurrentCell().getColumn();
            int row = h.getCurrentCell().getRow();
            Ability abil = h.getDodgeAbilities()[0];
            if (!abil.isReady()) continue;
            HeroThreats hts = myhts[i];
            if (hts.threats.size() == 0) continue;
            int drange = abil.getRange();
            double lowest_cell_damage = hts.max_dmg;
            int le1 = 0, le2 = 0;

            for (int e1 = (-drange); e1 < drange + 1; e1++) {
                for (int e2 = (-drange); e2 < drange + 1; e2++) {
                    int ncol = col + e1;
                    int nrow = row + e2;
                    if (Math.abs(e1) + Math.abs(e2) > drange) continue;
                    if (world.getMyHero(nrow, ncol) != null) continue;
                    if (nrow > -1 && nrow < row_n && ncol > -1 && ncol < col_n) {
                        double dmg = 0.0;
                        double dmg_noMul = 0.0;
                        Cell cell = world.getMap().getCell(nrow, ncol);
                        // So, dodgable cell...
                        if (!cell.isInObjectiveZone() && h.getCurrentCell().isInObjectiveZone()) dmg += 10.0;
                        boolean dg_bomb = false;
                        for (Threat thr : hts.threats) {
                            if (thr.isAffected(cell)) {
                                dmg += thr.pwr / thr.multiplicity;
                                dmg_noMul += thr.pwr;
                            }else{
                                if(thr.ability==AbilityName.BLASTER_BOMB) dg_bomb=true;
                            }
                        }
                        if (dmg < hts.max_dmg) {
                            double boost = 1.0;
                            // Keep Him Alive!
                            if (h.getCurrentHP() <= hts.noMul_max_dmg && h.getCurrentHP() > dmg_noMul) {
                                boost = 10.0;
                            }
                            if(dg_bomb) boost = 1.4;
                            // if(haveHealer) boost = 0.8;
                            dcellz.put(cell, (hts.max_dmg - dmg) * boost);
                        }
                    }
                }
            }
            if (dcellz.size() > 0) {
                HashMap<Cell, Double> result =
                        dcellz.entrySet().stream()
                                .sorted(java.util.Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                .collect(Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue,
                                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                // Keep 3 candidates
                for (int ind = 0; ind < 3; ind++) {
                    if (result.entrySet().iterator().hasNext()) {
                        java.util.Map.Entry<Cell, Double> entry = result.entrySet().iterator().next();
                        DgAction dga = new DgAction(h, entry.getKey(), world, entry.getValue());
                        dga.value *= 0.3;
                        hacts[i].acts.add(dga);
                    }
                }
            }

        }
        // TODO: REMOVE DODGES TO SAME SPOT => THIS SHOULDN'T PROPAGATE TO DECIDE PHASE
        // FORTIFY OPTIONS
        if (haveGuard) {
            for (i = 0; i < 4; i++) {
                Hero h = hacts[i].h;
                if (h.getName() != HeroName.GUARDIAN) continue;
                if (!h.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) continue;
                for (int j = 0; j < 4; j++) {
                    Hero dh = myhts[j].h;
                    if (world.manhattanDistance(h.getCurrentCell(), dh.getCurrentCell()) > 5) continue;
                    double max_dmg = myhts[j].max_dmg;
                    // TODO: PUT A LARGER/WISER THRESHOLD HERE, FORTIFY IS EXPENSIVE
                    if (max_dmg < 5.0) continue;
                    FtAction fta = new FtAction(h, dh.getCurrentCell(), dh, world, max_dmg);
                    if (DBG_FT) System.out.println(fta.toString());
                    hacts[i].acts.add(fta);
                }
            }
        }


        // DECIDE
        decide(hacts, world.getAP());

        // long t1 = System.currentTimeMillis();
        //  System.out.println("Elapsed: "+(t1-t0));
    }

    public void setRayable(Map map) {
        VisionTools vs = new VisionTools(map);
        int num = col_n * row_n;
        rayable = new boolean[num][num];
        // TODO: Memoizing for speedup
        for (int i = 0; i < num; i++) {
            Cell c1 = map.getCell(i / col_n, i % col_n);
            if (c1.isWall()) continue;
            for (int j = i + 1; j < num; j++) {
                Cell c2 = map.getCell(j / col_n, j % col_n);
                boolean val = vs.canHit(c1, c2);
                rayable[i][j] = val;
                rayable[j][i] = val;
            }
        }
    }

    public class HeroActs {
        public Hero h;
        public int index;
        public ArrayList<Action> acts;

        public HeroActs(Hero h) {
            this.h = h;
        }

        public HeroActs(Hero h, int index) {
            this.h = h;
            this.index = index;
        }
    }

    public class HeroThreats {
        double noMul_max_dmg;
        double max_dmg;
        public Hero h;
        public ArrayList<Threat> threats;

        public HeroThreats(Hero h) {
            this.threats = new ArrayList<>();
            this.h = h;
            this.max_dmg = 0.0;
            this.noMul_max_dmg = 0.0;
        }
    }


    public void setUpHts(HeroThreats ht, HeroThreats[] myhts) {
        AbilityTools atool = new AbilityTools(world.getMap(), Arrays.asList(world.getMyHeroes()), Arrays.asList(world.getOppHeroes()));
        ht.threats = new ArrayList<>();
        if (ht.h.getCurrentHP() == 0) return;
        if (ht.h.getCurrentCell().getRow() == -1) return;
        switch (ht.h.getName()) {
            case SHADOW:{

                break;
            }
            case GUARDIAN: {
                // Guardian Attacks
                for (Direction dir : Direction.values()) {
                    int hit_num = 0;
                    Cell tcell = fws.nxtCell(ht.h.getCurrentCell(), dir, world.getMap());
                    Hero[] heros = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.GUARDIAN_ATTACK), ht.h.getCurrentCell(), tcell);
                    hit_num = heros.length;
                    if (hit_num == 0) continue;
                    Threat thr = new Threat(ht.h, heros, ht.h.getAbility(AbilityName.GUARDIAN_ATTACK).getPower(), tcell, AbilityName.GUARDIAN_ATTACK);
                    for (Hero uah : heros) {
                        myhts[my_hero_to_ind.get(uah)].threats.add(thr);
                    }
                    ht.threats.add(thr);
                }
                // Same Cell Attack
                int hit_num = 0;
                Cell tcell = ht.h.getCurrentCell();
                Hero[] heros = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.GUARDIAN_ATTACK), ht.h.getCurrentCell(), tcell);
                hit_num = heros.length;
                if (hit_num == 0) break;
                Threat thr = new Threat(ht.h, heros, ht.h.getAbility(AbilityName.GUARDIAN_ATTACK).getPower(), tcell, AbilityName.GUARDIAN_ATTACK);
                for (Hero uah : heros) {
                    myhts[my_hero_to_ind.get(uah)].threats.add(thr);
                }
                ht.threats.add(thr);
                break;
            }
            case SENTRY: {
                for (Hero eh : world.getMyHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    Hero[] herosAt = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.SENTRY_ATTACK), eh.getCurrentCell(), tcell);
                    Hero[] herosRay = new Hero[0];
                    if (oppHeros.get(ht.h.getId()).special_rem == 0) {
                        herosRay = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.SENTRY_RAY), eh.getCurrentCell(), tcell);
                    }
                    if (herosAt.length > 0) {
                        int pwr = ht.h.getAbility(AbilityName.SENTRY_ATTACK).getPower();
                        AbilityTools at = new AbilityTools(world.getMap(), Arrays.asList(world.getOppHeroes()), Arrays.asList(new Hero[]{}));
                        Threat thr = new Threat(ht.h, herosAt, pwr, tcell, AbilityName.SENTRY_ATTACK, at);
                        myhts[my_hero_to_ind.get(herosAt[0])].threats.add(thr);
                        ht.threats.add(thr);
                    }
                    if (herosRay.length > 0) {
                        int pwr = ht.h.getAbility(AbilityName.SENTRY_RAY).getPower();
                        AbilityTools at = new AbilityTools(world.getMap(), Arrays.asList(world.getOppHeroes()), Arrays.asList(new Hero[]{}));
                        Threat thr = new Threat(ht.h, herosAt, pwr, tcell, AbilityName.SENTRY_RAY, at);
                        myhts[my_hero_to_ind.get(herosAt[0])].threats.add(thr);
                        ht.threats.add(thr);
                    }
                }
                break;
            }
            case HEALER: {
                for (Hero eh : world.getMyHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    if (world.manhattanDistance(ht.h.getCurrentCell(), tcell) > 4) continue;
                    int pwr = ht.h.getAbility(AbilityName.HEALER_ATTACK).getPower();
                    Threat thr = new Threat(ht.h, eh, pwr, tcell, AbilityName.HEALER_ATTACK);
                    myhts[my_hero_to_ind.get(eh)].threats.add(thr);
                    ht.threats.add(thr);
                }
                break;
            }
            case BLASTER: {
                for (Hero eh : world.getMyHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    if (world.manhattanDistance(ht.h.getCurrentCell(), tcell) > 4) continue;
                    int pwr = ht.h.getAbility(AbilityName.BLASTER_ATTACK).getPower();
                    // TODO: HARD-CASE => WEIRD DODGE CHOICE: when you dodge 1 unit which is behind a unit IN PATH (which does not dodge)
                    // TODO: have to discard this case AND [THIS AND IS IMPORTANT] give more credit to another choice which uses this cover
                    // TODO: Handle dodges for cases when attack target IS BETWEEN your troops for MULTIDAMAGE
                    Hero[] herosAt = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.BLASTER_ATTACK), eh.getCurrentCell(), tcell);
                    Threat thr = new Threat(ht.h, herosAt, pwr, tcell, AbilityName.BLASTER_ATTACK, atool);
                    for (Hero uah : herosAt) {
                        myhts[my_hero_to_ind.get(uah)].threats.add(thr);
                    }
                    ht.threats.add(thr);
                    if (oppHeros.get(ht.h.getId()).special_rem == 0) {
                        if (world.manhattanDistance(ht.h.getCurrentCell(), tcell) > 7) continue;
                        pwr = ht.h.getAbility(AbilityName.BLASTER_BOMB).getPower();
                        herosAt = atool.getENEMYAbilityTargets(ht.h.getAbility(AbilityName.BLASTER_BOMB), eh.getCurrentCell(), tcell);
                        if (herosAt.length == 0) continue;
                        Threat thr2 = new Threat(ht.h, herosAt, pwr, tcell, AbilityName.BLASTER_BOMB, atool);
                        for (Hero uah : herosAt) {
                            myhts[my_hero_to_ind.get(uah)].threats.add(thr2);
                        }
                        ht.threats.add(thr2);
                    }
                }
                break;
            }
        }
    }

    public void setUpAcActs(HeroActs ha) {
        ha.acts = new ArrayList<>();
        // int cur_ind = fws.toInd(ha.h.getCurrentCell());
        // Add NOP Action
        Action ac = new AtAction(ha.h, world, this);
        ha.acts.add(ac);
        if (ha.h.getCurrentHP() == 0) return;
        switch (ha.h.getName()) {
            case SHADOW:{
                int crow = ha.h.getCurrentCell().getRow();
                int ccol = ha.h.getCurrentCell().getColumn();
                if(ha.h.getAbility(AbilityName.SHADOW_SLASH).isReady()){
                    Cell bestcell=null;
                    Hero[] targs =null;
                    double maxval = -100.0;
                    for(Hero eh: world.getOppHeroes()){
                        Cell tcell = eh.getCurrentCell();
                        Hero[] heros = world.getAbilityTargets(AbilityName.SHADOW_SLASH, ha.h.getCurrentCell(), tcell);
                        if(heros.length>0){
                            double val = heros.length*50000.5;
                            if(val>maxval){
                                bestcell = tcell;
                                maxval = val;
                                targs = heros;
                            }
                        }
                    }
                    if(bestcell!=null){
                        AtAction atac = new AtAction(ha.h,world,bestcell,ha.h.getAbility(AbilityName.SHADOW_SLASH),targs);
                        atac.value=maxval;
                        ha.acts.add(atac);
                    }
                }
                if(ha.h.getAbility(AbilityName.SHADOW_ATTACK).isReady()){
                    for(Hero eh: world.getOppHeroes()){
                        if(world.manhattanDistance(eh.getCurrentCell(),ha.h.getCurrentCell())<2){
                            AtAction atac = new AtAction(ha.h,world,eh.getCurrentCell(),ha.h.getAbility(AbilityName.SHADOW_ATTACK));
                            atac.value = 400.0;
                        }
                    }
                }
                break;
            }
            case GUARDIAN: {
                // Guardian Attacks
                for (Direction dir : Direction.values()) {
                    int hit_num = 0;
                    Cell tcell = fws.nxtCell(ha.h.getCurrentCell(), dir, world.getMap());
                    Hero[] heros = world.getAbilityTargets(AbilityName.GUARDIAN_ATTACK, ha.h.getCurrentCell(), tcell);
                    hit_num = heros.length;
                    if (hit_num == 0) continue;
                    AtAction atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.GUARDIAN_ATTACK), heros);
                    atac.price = ha.h.getAbility(AbilityName.GUARDIAN_ATTACK).getAPCost();
                    atac.value = ha.h.getAbility(AbilityName.GUARDIAN_ATTACK).getPower() * hit_num;
                    if (DBG_ATK) System.out.println(atac.toString());
                    ha.acts.add(atac);
                }
                // Same Cell Attack
                int hit_num = 0;
                Cell tcell = ha.h.getCurrentCell();
                Hero[] heros = world.getAbilityTargets(AbilityName.GUARDIAN_ATTACK, ha.h.getCurrentCell(), tcell);
                hit_num = heros.length;
                if (hit_num == 0) break;
                AtAction atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.GUARDIAN_ATTACK), this);
                atac.price = ha.h.getAbility(AbilityName.GUARDIAN_ATTACK).getAPCost();
                atac.value = ha.h.getAbility(AbilityName.GUARDIAN_ATTACK).getPower() * hit_num;
                if (DBG_ATK) System.out.println(atac.toString());
                ha.acts.add(atac);
                break;
            }
            case SENTRY: {
                // Sentry Attacks
                for (Hero eh : world.getOppHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    Hero[] herosAt = world.getAbilityTargets(AbilityName.SENTRY_ATTACK, ha.h.getCurrentCell(), tcell);
                    Hero[] herosRay = new Hero[0];
                    if (ha.h.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        herosRay = world.getAbilityTargets(AbilityName.SENTRY_RAY, ha.h.getCurrentCell(), tcell);
                    }
                    if (herosAt.length > 0) {
                        AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.SENTRY_ATTACK), this);
                        n_atac.price = ha.h.getAbility(AbilityName.SENTRY_ATTACK).getAPCost();
                        n_atac.value = ha.h.getAbility(AbilityName.SENTRY_ATTACK).getPower();
                        if (DBG_ATK) System.out.println(n_atac.toString());
                        ha.acts.add(n_atac);
                    }
                    if (herosRay.length > 0) {
                        AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.SENTRY_RAY), this);
                        n_atac.price = ha.h.getAbility(AbilityName.SENTRY_RAY).getAPCost();
                        n_atac.value = ha.h.getAbility(AbilityName.SENTRY_RAY).getPower();
                        if (DBG_ATK) System.out.println(n_atac.toString());
                        ha.acts.add(n_atac);
                    }
                }
                break;
            }
            case BLASTER: {
                for (Hero eh : world.getOppHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    if (tcell.getRow() == -1) continue;
                    Hero[] herosAt = world.getAbilityTargets(AbilityName.BLASTER_ATTACK, ha.h.getCurrentCell(), tcell);
                    if (herosAt.length > 0) {
                        AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.BLASTER_ATTACK), herosAt);
                        // Kill Weak Faster!
//                        for (Hero h : herosAt) {
//                            if (h.getCurrentHP() < ha.h.getAbility(AbilityName.BLASTER_ATTACK).getPower())
//                                val+=50;
//                            else if(h.getCurrentHP()<100) val += 30;
//                            else if(h.getCurrentHP()<150) val += 20;
//                        }
                        n_atac.price = ha.h.getAbility(AbilityName.BLASTER_ATTACK).getAPCost();
                        n_atac.value = ha.h.getAbility(AbilityName.BLASTER_ATTACK).getPower() * herosAt.length ;
                        if (DBG_ATK) System.out.println(n_atac.toString());
                        ha.acts.add(n_atac);
                    }
                }
                // BOMB_ATTACK
                if (!ha.h.getAbility(AbilityName.BLASTER_BOMB).isReady()) break;
                int total_added = 0;
                Cell myCell = ha.h.getCurrentCell();
                int myrow = ha.h.getCurrentCell().getRow();
                int mycol = ha.h.getCurrentCell().getColumn();
                HashMap<Integer, HashSet<Integer>> vicin = new HashMap<>();
                for (int i = 0; i < 4; i++) {
                    HashSet<Integer> hset = new HashSet<>();
                    vicin.put(i, hset);
                    Hero eh = world.getOppHeroes()[i];
                    int row = eh.getCurrentCell().getRow();
                    int col = eh.getCurrentCell().getColumn();
                    if (row == -1) continue;
                    if (eh.getCurrentHP() == 0) continue;
                    if (world.manhattanDistance(myCell, eh.getCurrentCell()) > 7) continue;
                    for (int e1 = -2; e1 < 3; e1++) {
                        for (int e2 = -2; e2 < 3; e2++) {
                            int nrow = row + e1;
                            int ncol = col + e2;
                            if (Math.abs(myrow - nrow) + Math.abs(mycol - ncol) > 5) continue;
                            if (Math.abs(e1) + Math.abs(e2) <= 2 && nrow > -1 && nrow < row_n && ncol > -1 && ncol < col_n) {
                                hset.add(nrow * col_n + ncol);
                                total_added++;
                            }
                        }
                    }
                    vicin.put(i, hset);
                }
                if (total_added == 0) break;
                // 4SOME
                HashSet<Integer> inter4 = new HashSet<>(vicin.get(0));
                inter4.retainAll(vicin.get(1));
                inter4.retainAll(vicin.get(2));
                inter4.retainAll(vicin.get(3));
                if (inter4.size() > 0) {
                    int tind = inter4.iterator().next();
                    Cell tcell = world.getMap().getCell(tind / col_n, tind % col_n);
                    AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.BLASTER_BOMB), this);
                    n_atac.price = ha.h.getAbility(AbilityName.BLASTER_BOMB).getAPCost();
                    n_atac.value = ha.h.getAbility(AbilityName.BLASTER_BOMB).getPower() * 4;
                    if (DBG_ATK) System.out.println(n_atac.toString());
                    ha.acts.add(n_atac);
                    break;
                }
                // 3SOME
                // TODO: survey edge-cases, decide between MULTIPLE choice of 3 enemy, CROSS-CALCULATION of Attacks
                ArrayList<HashSet<Integer>> inter3s = new ArrayList<>();
                for (int i1 = 0; i1 < 2; i1++) {
                    for (int i2 = i1 + 1; i2 < 3; i2++) {
                        for (int i3 = i2 + 1; i3 < 4; i3++) {
                            HashSet<Integer> hset = new HashSet<>(vicin.get(i1));
                            hset.retainAll(vicin.get(i2));
                            hset.retainAll(vicin.get(i3));
                            if (hset.size() > 0) {
                                inter3s.add(hset);
                                int tind = hset.iterator().next();
                                Cell tcell = world.getMap().getCell(tind / col_n, tind % col_n);
                                AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.BLASTER_BOMB), this);
                                n_atac.price = ha.h.getAbility(AbilityName.BLASTER_BOMB).getAPCost();
                                n_atac.value = ha.h.getAbility(AbilityName.BLASTER_BOMB).getPower() * 3;
                                if (DBG_ATK) System.out.println(n_atac.toString());
                                ha.acts.add(n_atac);
                                break;
                            }
                        }
                    }
                }
                // 2SOME
                ArrayList<HashSet<Integer>> inter2s = new ArrayList<>();
                for (int i1 = 0; i1 < 3; i1++) {
                    for (int i2 = i1 + 1; i2 < 4; i2++) {
                        HashSet<Integer> hset = new HashSet<>(vicin.get(i1));
                        hset.retainAll(vicin.get(i2));
                        if (hset.size() > 0) {
                            inter2s.add(hset);
                            int tind = hset.iterator().next();
                            Cell tcell = world.getMap().getCell(tind / col_n, tind % col_n);
                            AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.BLASTER_BOMB), this);
                            n_atac.price = ha.h.getAbility(AbilityName.BLASTER_BOMB).getAPCost();
                            n_atac.value = ha.h.getAbility(AbilityName.BLASTER_BOMB).getPower() * 2;
                            if (DBG_ATK) System.out.println(n_atac.toString());
                            ha.acts.add(n_atac);
                            break;
                        }
                    }
                }
                // 1SOME
                for (HashSet<Integer> hset : vicin.values()) {
                    if (hset.size() > 0) {
                        int tind = hset.iterator().next();
                        Cell tcell = world.getMap().getCell(tind / col_n, tind % col_n);
                        AtAction n_atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.BLASTER_BOMB), this);
                        n_atac.price = ha.h.getAbility(AbilityName.BLASTER_BOMB).getAPCost();
                        n_atac.value = ha.h.getAbility(AbilityName.BLASTER_BOMB).getPower();
                        if (DBG_ATK) System.out.println(n_atac.toString());
                        ha.acts.add(n_atac);
                        break;
                    }
                }
                break;
            }
            case HEALER: {
                if (!ha.h.getAbility(AbilityName.HEALER_HEAL).isReady()) break;
                if ((ha.h.getMaxHP() - ha.h.getCurrentHP()) > 39) {
                    HeAction heac = new HeAction(ha.h, ha.h.getCurrentCell(), world, 50.0);
                    ha.acts.add(heac);
                    break;
                }
                for (Hero h : world.getMyHeroes()) {
                    if (h.getId() == ha.h.getId()) continue;
                    if (world.manhattanDistance(h.getCurrentCell(), ha.h.getCurrentCell()) < 5) {
                        int dhp = h.getMaxHP() - h.getCurrentHP();
                        if (dhp < 40) continue;
                        double val = (double) dhp;
                        if (!h.getDodgeAbilities()[0].isReady()) val = val * 1.2;
                        HeAction heac = new HeAction(ha.h, h.getCurrentCell(), world, val);
                        System.out.println("HEAL OPT ADDED!");
                        ha.acts.add(heac);
                    }
                }
                for (Hero eh : world.getOppHeroes()) {
                    Cell tcell = eh.getCurrentCell();
                    if (world.manhattanDistance(tcell, ha.h.getCurrentCell()) > 4) continue;
                    AtAction atac = new AtAction(ha.h, world, tcell, ha.h.getAbility(AbilityName.HEALER_ATTACK));
                    atac.value = ha.h.getAbility(AbilityName.HEALER_ATTACK).getPower();
                    ha.acts.add(atac);
                }
                break;
            }

            default:
                break;
        }


    }

    public void setUpMvActs(HeroActs ha) {
//        if(haveHealer && healer_id==-1){
//            for(int i=0;i<4;i++){
//                Hero h = world.getMyHeroes()[i];
//                if(h.getName()==HeroName.HEALER){
//                    healer_id=i;
//                    break;
//                }
//            }
//        }
        ha.acts = new ArrayList<>();
        int cur_ind = fws.toInd(ha.h.getCurrentCell());
        // Add NOP Action
        MvAction ac = new MvAction(ha.h, world, this);
        if (DBG_MVM) System.out.println(ac.toString());
        ha.acts.add(ac);

//        if(shiftFREQS && world.getCurrentTurn()>60 && world.getCurrentTurn()<101 && (world.getCurrentTurn()<80-avg_ttime && Math.abs(world.getCurrentTurn()-80-avg_ttime)<shift_window)){
//            System.out.println("F");
//            if(ha.h.getCurrentCell().isInMyRespawnZone()) return;
//        }
        if (ha.h.getCurrentHP() == 0) return;
        ArrayList<Integer> bombers = new ArrayList<>();
        ArrayList<Integer> norms = new ArrayList<>();
        for(Hero eh : world.getOppHeroes()){
            if(oppHeros==null || !(oppHeros.containsKey(eh.getId()))) continue;
            if(eh.getName()==HeroName.BLASTER && oppHeros.get(eh.getId()).special_rem==0 && eh.getCurrentHP()!=0 && eh.getCurrentCell().getRow()!=-1){
                bombers.add(fws.toInd(eh.getCurrentCell()));
            }
        }
        int[] blist = bombers.stream().mapToInt(i -> i).toArray();
        if(ha.h.getCurrentHP()<36 && ha.h.getDodgeAbilities()[0].isReady()) {
            if(blist.length>0){
                for (Direction d : Direction.values()) {
                    int nxt_ind = fws.nxtInd(cur_ind, d);
                    if (nxt_ind == -1) continue;
                    Cell curCell = ha.h.getCurrentCell();
                    Cell nxtCell = fws.nxtCell(curCell, d, world.getMap());
                    ac = new MvAction(ha.h, world, d, this);
                    int cdist = fws.getDistClosestInSet(cur_ind,blist);
                    int ndist = fws.getDistClosestInSet(nxt_ind,blist);
                    if(ndist<cdist){
                        ac.value+=15.0;
                        ha.acts.add(ac);
                        // System.out.println("D");
                        return;
                    }
                }
            }
        }
        // Healer Dont Rush
        // if(ha.h.getName() == HeroName.HEALER && world.getCurrentTurn()<5) return;
        if (!ha.h.getCurrentCell().isInObjectiveZone() && ha.h.getDodgeAbilities()[0].isReady() && world.getMovePhaseNum() == 0) {
            int row = cur_ind / world.getMap().getColumnNum();
            int col = cur_ind % world.getMap().getColumnNum();
            int drange = ha.h.getDodgeAbilities()[0].getRange();
            // CAUTION: THE FOLLOWING LINE ASSUMES BLASTER - THE 6 IS HARD-CODED
            if(obj_zone_indices == null){
               // System.out.println("NULL OBJZ!");
                obj_zone_indices = new int[]{450};
            }
            int curDist = fws.getDistClosestInSet(cur_ind, obj_zone_indices);
            int best_n_dist = curDist - 6;
            int best_row = -1, best_col = -1;
            int best_ind = -1;
            for (int e1 = -drange; e1 < drange; e1++) {
                for (int e2 = -drange; e2 < drange; e2++) {
                    int nrow = row + e1;
                    int ncol = col + e2;
                    int nind = nrow * world.getMap().getColumnNum() + ncol;
                    if (nrow < 0 || nrow >= world.getMap().getRowNum() || ncol < 0 || ncol >= world.getMap().getColumnNum())
                        continue;
                    if (Math.abs(e1) + Math.abs(e2) <= drange) {
                        Cell tcell = world.getMap().getCell(nrow, ncol);
                        if (tcell.isWall()) continue;
                        int tind = fws.toInd(tcell);
                        if (dodging_pts.contains(tind)) continue;
                        int nxtdist = fws.getDistClosestInSet(tind, obj_zone_indices);
                        if (nxtdist < best_n_dist) {
                            best_n_dist = nxtdist;
                            best_col = ncol;
                            best_row = nrow;
                            best_ind = nind;
                        }
                    }
                }
            }
            if (best_ind != -1) {
                dodging_pts.add(best_ind);
                // System.out.println("DODGE QUEUE++");
                dodge_queue.add(new dodge_stub(ha.index, best_ind));
                dodger[ha.index] = true;
            }
        }
        if (dodger[ha.index]) return;
        for (Direction d : Direction.values()) {
            int nxt_ind = fws.nxtInd(cur_ind, d);
            if (nxt_ind == -1) continue;
            Cell curCell = ha.h.getCurrentCell();
            Cell nxtCell = fws.nxtCell(curCell, d, world.getMap());
            ac = new MvAction(ha.h, world, d, this);
            if (expectingBattle()) ac.value = ac.value - 0.5;
            else ac.value = ac.value - 0.5;
            switch (ha.h.getName()) {
                case SHADOW:
                case HEALER:
                case BLASTER: {
                    boolean dontPut = false;
                    if(ha.h.getName()==HeroName.SHADOW){
                        if(ha.h.getAbility(AbilityName.SHADOW_SLASH).isReady()){
                            int cdist = fws.getDistClosestInSet(cur_ind,enemy_inds);
                            int ndist = fws.getDistClosestInSet(nxt_ind,enemy_inds);
                            if(ndist<cdist && ndist>2){
                                ac.value+=1000.0;
                            }
                            if(ndist>cdist && ndist>5) ac.value-=1000.0;
                        }else{
                            int cdist = fws.getDistClosestInSet(cur_ind,enemy_inds);
                            int ndist = fws.getDistClosestInSet(nxt_ind,enemy_inds);
                            if(ndist>cdist && ndist<8){
                                ac.value+=1000.0;
                            }
                            if(ndist<cdist && ndist<7) ac.value-=1000.0;

                        }
                    }else{
                        if (curCell.isInObjectiveZone()) {
                            if (!nxtCell.isInObjectiveZone()) {
                                ac.inToOut = true;
                                ac.value -= 1.0;
                            } else {
                                ac.inToIn = true;
                                // No need to charge
                                if(ha.h.getName()==HeroName.SHADOW){
                                    // RUSH
                                    if(enemies_in>0){
                                        int cdist = fws.getDistClosestInSet(cur_ind,enemy_inds);
                                        int ndist = fws.getDistClosestInSet(nxt_ind,enemy_inds);
                                        if(ndist<cdist && cdist>1){
                                            ac.value+=1000.0;
                                        }
                                        if(ndist>cdist) ac.value-=1000.0;
                                    }

                                }
                                if (ha.h.getCurrentHP() > (0.6) * ha.h.getMaxHP()) {
                                    int cdist = fws.getDistClosestInSet(cur_ind, opprs_indices);
                                    int nxtdist = fws.getDistClosestInSet(nxt_ind, opprs_indices);
                                    if (nxtdist < cdist) {
                                        if (enemies_in == 0 && enemies_incoming == 0) {
                                            ac.value += 0.95;
                                        } else {
                                            if (enemies_in == 0 && world.getMovePhaseNum() > 3) ac.value += 0.95;
                                        }
                                    }
                                } else {
//                                if(ha.h.getCurrentHP()<60 && haveHealer){
//                                    Hero healer = world.getHero(healer_id);
//                                    if(healer.getCurrentHP()==0) continue;
//                                    int cdist= world.manhattanDistance(curCell,healer.getCurrentCell());
//                                    int ndist =(int) fws.dp[nxt_ind][fws.toInd(healer.getCurrentCell())];
//                                    if(nxt_ind%col_n==healer.getCurrentCell().getColumn() || nxt_ind/col_n==healer.getCurrentCell().getRow()) continue;
//                                    if(ndist<cdist && ndist>2){
//                                        ac.value+=6.0;
//                                    }
//                                }
                                }

                            }
                        } else {
                            int curDist = fws.getDistClosestInSet(cur_ind, obj_zone_indices);
                            int nxtDist = fws.getDistClosestInSet(nxt_ind, obj_zone_indices);
                            if (curDist > nxtDist) {
                                ac.value += 15.0;
                                if (ha.h.getName() == HeroName.GUARDIAN) {
                                    ac.value -= 0.1;
                                } else if (ha.h.getName() == HeroName.SENTRY) {
                                    ac.value += 0.1;
                                }
                            } else if (curDist < nxtDist) {
                                ac.value -= 15.0;
                            }
                            if (nxtCell.isInObjectiveZone()) ac.value += 5.0;
                        }
                    }

                    break;
                }
                case GUARDIAN: {
//                    int curDistToE=-1; int nxtDistToE=-1;
//                    if(!noEnemeyInd){
//                         curDistToE = fws.getDistClosestInSet(cur_ind,enemy_inds);
//                         nxtDistToE = fws.getDistClosestInSet(nxt_ind,enemy_inds);
//                    }
//
//                    if(wounded_inds.size()>0 && ha.h.getAbility(AbilityName.HEALER_HEAL).isReady()){
//                        int cdi = (int)fws.dp[wounded_inds.get(0)][cur_ind];
//                        int ndi= (int) fws.dp[wounded_inds.get(0)][nxt_ind];
//                        if(ndi<cdi && ndi>2){
//                            ac.value+=8.0;
//                        }
//                    }else{
//                        if(!noEnemeyInd){
//                            if(nxtDistToE<curDistToE && nxtDistToE<7) continue;
//                            if(curDistToE<7 && nxtDistToE>curDistToE){
//                                ac.value+=10.0;
//                                continue;
//                            }
//                        }
//                    }
//
//
//                    if(curCell.isInObjectiveZone()){
//                        if(!nxtCell.isInObjectiveZone()){
//                            ac.value -= 1.0;
//                        }else{
//
//                        }
//                    }else{
//                        int curDist = fws.getDistClosestInSet(cur_ind,obj_zone_indices);
//                        int nxtDist = fws.getDistClosestInSet(nxt_ind,obj_zone_indices);
//                        if(curDist>nxtDist){
//                            ac.value += 15.0;
//                        }else if(curDist<nxtDist){
//                            ac.value -= 15.0;
//                        }
//                        if(nxtCell.isInObjectiveZone()) ac.value +=5.0;
//                    }
                    break;
                }
            }

            if (DBG_MVM) System.out.println(ac.toString());
            if (ac.inToOut) continue;
            ha.acts.add(ac);
        }

    }

    public class Choice {
        HeroActs[] hacts;
        int[] inds;
        double value;

        public Choice(HeroActs[] hacts, int[] inds, int apower) {
            this.hacts = hacts;
            this.inds = inds;
            int price = 0;
            value = 0.0;
            for (int i = 0; i < 4; i++) {
                price += hacts[i].acts.get(inds[i]).price;
                value += hacts[i].acts.get(inds[i]).value;
            }
            if (price > apower) {
                value = -1000000000.0;
            }
        }

        public void execute() {
            for (int i = 0; i < 4; i++) {
                hacts[i].acts.get(inds[i]).execute();
            }
        }

        public void execute(AI ai) {
            ai.last_fighters = 0;
            for (int i = 0; i < 4; i++) {
                Action a = hacts[i].acts.get(inds[i]);
                if (a instanceof AtAction || a instanceof FtAction || a instanceof DgAction) ai.last_fighters++;
                a.execute();
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(hacts[i].acts.get(inds[i]).toString());
            }
            return sb.toString();
        }
    }

    public void decide(HeroActs[] hacts, int apower) {
        HashMap<Choice, Double> choice_table = new HashMap<>();
        int[][] D = new int[4][4];
        int[] Dgist = new int[5];
        int prev_range = 0;
        int prev_closest = 1000;
        if (standApart) {

            for (int i = 0; i < 4; i++) {
                for (int j = i + 1; j < 4; j++) {
                    if (i == j) {
                        D[i][j] = 0;
                        continue;
                    }
                    Cell ci = hacts[i].h.getCurrentCell();
                    Cell cj = hacts[j].h.getCurrentCell();
                    if (ci.isInObjectiveZone() && cj.isInObjectiveZone() && hacts[i].h.getCurrentHP() != 0 && hacts[j].h.getCurrentHP() != 0) {
                        D[i][j] = world.manhattanDistance(ci, cj);
                        Dgist[Math.min(4, (D[i][j] - 1))]++;
                        D[j][i] = D[i][j];
                    } else {
                        D[i][j] = 5;
                        D[j][i] = 5;
                        Dgist[4]++;
                    }
                }
            }
        }
        for (int i0 = 0; i0 < hacts[0].acts.size(); i0++) {
            for (int i1 = 0; i1 < hacts[1].acts.size(); i1++) {
                for (int i2 = 0; i2 < hacts[2].acts.size(); i2++) {
                    for (int i3 = 0; i3 < hacts[3].acts.size(); i3++) {
                        int[] inds = new int[]{i0, i1, i2, i3};
                        Choice ch = new Choice(hacts, inds, apower);
                        // IF MOVE PHASE - FIX PATH BLOCKING ENTANGLEMENT
                        // InToOut choices DON'T EXIST!
                        if (world.getCurrentPhase() == Phase.MOVE) {
                            boolean crashed = false;
                            for (int i = 0; i < 4; i++) {
                                MvAction mva = (MvAction) hacts[i].acts.get(inds[i]);
                                if (!mva.isNOP) {
                                    for (int j = 0; j < 4; j++) {
                                        if (j == i) continue;
                                        MvAction mva2 = (MvAction) hacts[j].acts.get(inds[j]);
                                        if (mva2.isNOP) {
                                            if (mva.nxtpos == mva2.curpos) {
                                                ch.value -= 10000.0;
                                                crashed = true;
                                                break;
                                            }
                                        } else {
                                            // swapping positions is stupid
                                            if (mva.nxtpos == mva2.curpos && mva.curpos == mva2.nxtpos) {
                                                ch.value -= 10000.0;
                                                crashed = true;
                                                break;
                                            }
                                        }

                                    }
                                    if (crashed) break;
                                }
                            }
                            if (lookAhead) {

                            }
                            if (!crashed && standApart) {
                                if (standApart) {
                                    double maxStPrize = 4.0;
                                    double minStPrize = 2.0;
                                    double aux = 0.0;
                                    int[] Dsgist = new int[5];
                                    int[][] Ds = Ds(hacts, ch.inds, Dsgist);

                                    for (int i = 0; i < 5; i++) {
                                        if (Dsgist[4] > Dgist[4]) aux += (Dsgist[4] - Dgist[4]) * 10.0;
                                        else if (Dsgist[4] == Dgist[4]) {
                                            aux += (Dsgist[3] - Dgist[3]) * 3.0;
                                            if (Dsgist[3] == Dgist[3]) {
                                                aux += (Dsgist[2] - Dgist[2]) * 2.0;
                                                if (Dsgist[2] == Dgist[2]) {
                                                    aux += (Dsgist[1] - Dgist[1]) * 1.5;
                                                } else {
                                                    ch.value -= 2.0;
                                                }
                                            } else {
                                                ch.value -= 2.0;
                                            }
                                        } else {
                                            ch.value -= 2.0;
                                        }
                                        ch.value += aux;
                                    }
                                    ch.value += aux * 3.2;
                                    // System.out.println("chval: "+ch.value+" delta cover: "+(hinf[0]-prev_range));
                                }
                            }
                        }
                        // IF ACTION PHASE - FIX DODGE/FORTIFY BLOCKING ENTANGLEMENT
                        if (world.getCurrentPhase() == Phase.ACTION) {
                            if (haveGuard)
                            {
                                boolean crashed = false;
                                for (int i = 0; i < 4; i++) {
                                    Action act = hacts[i].acts.get(inds[i]);
                                    if (act instanceof FtAction) {
                                        for (int j = 0; j < 4; j++) {
                                            if (j == i) continue;
                                            Action act2 = hacts[j].acts.get(inds[j]);
                                            if (act2 instanceof FtAction) {
                                                if (((FtAction) act).targetCell == ((FtAction) act2).targetCell) {
                                                    ch.value -= 10000.0;
                                                    crashed = true;
                                                    break;
                                                }
                                            }
                                            if (act2 instanceof DgAction) {
                                                if (((FtAction) act).targetHero == ((DgAction) act2).hero) {
                                                    ch.value -= 10000.0;
                                                    crashed = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (crashed) break;
                                    }
                                    // Dodge-Heal
                                    else if (act instanceof HeAction) {
                                        for (int j = 0; j < 4; j++) {
                                            if (j == i) continue;
                                            Action act2 = hacts[j].acts.get(inds[j]);
                                            if (act2 instanceof DgAction) {
                                                if (((HeAction) act).targetCell == ((DgAction) act2).hero.getCurrentCell()) {
                                                    ch.value -= 10000.0;
                                                    crashed = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (crashed) break;
                                    }
                                }
                            }
                            // END IF HAVE_GUARD
                            // DODGE ENTANGLEMENT - DODGE TO SAME CELL
                            boolean crashed = false;
                            for (int i = 0; i < 4; i++) {
                                Action act = hacts[i].acts.get(inds[i]);
                                if (act instanceof DgAction) {
                                    for (int j = 0; j < 4; j++) {
                                        if (j == i) continue;
                                        Action act2 = hacts[j].acts.get(inds[j]);
                                        if (act2 instanceof DgAction) {
                                            if (((DgAction) act).targetCell == ((DgAction) act2).targetCell) {
                                                ch.value -= Math.max(act.value, act2.value);
                                            }
                                        }

                                    }
                                    if (crashed) break;
                                }
                            }
                            // Hit Analysis
                            int kills=0;
                            int u60=0;
                            int u100=0;
                            int[] ehit = new int[]{0,0,0,0};
                            int[] edmg = new int[]{0,0,0,0};
                            int[] multi_hit = new int[]{0,0,0,0,0};
                            for(int i=0;i<4;i++){
                                if(hacts[i].acts.get(inds[i]) instanceof AtAction){
                                    AtAction atac = (AtAction)hacts[i].acts.get(inds[i]);
                                    if(atac.isNOP) continue;
                                    if(atac.target!=null){
                                        for(Hero eh:atac.targs){
                                            if(opp_hero_to_ind.containsKey(eh)){
                                                int ind = opp_hero_to_ind.get(eh);
                                                ehit[ind]++;
                                                edmg[ind]+=atac.abil.getPower();
                                            }
                                        }
                                    }
                                }
                            }
                            for(int i=0;i<4;i++){
                                int newhp = world.getOppHeroes()[i].getCurrentHP()-edmg[i];
                                if(newhp<1) kills++;
                                else if(newhp<60) u60++;
                                else if(newhp<100) u100++;
                                multi_hit[ehit[i]]++;
                            }
                            ch.value+=kills*1000.0+u60*220.0+u100*50.0+multi_hit[2]*3+multi_hit[3]*9;
                        }
                        // DECIDE
                        choice_table.put(ch, ch.value);
                    }
                }
            }
        }
        // Find best choice
        HashMap<Choice, Double> sorted = choice_table
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(java.util.Map.Entry.comparingByValue()))
                .collect(
                        java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));
        Choice best_choice = sorted.keySet().iterator().next();
        best_choice.execute();
    }

    public void set_ind_arrays(World world) {
        obj_zone_indices = new int[world.getMap().getObjectiveZone().length];
        int ci = 0;
        for (Cell c : world.getMap().getObjectiveZone()) {
            obj_zone_indices[ci] = fws.toInd(c.getRow(), c.getColumn());
            ci++;
        }
        ci = 0;
        myrs_indices = new int[world.getMap().getMyRespawnZone().length];
        for (Cell c : world.getMap().getMyRespawnZone()) {
            myrs_indices[ci] = fws.toInd(c.getRow(), c.getColumn());
            ci++;
        }
        ci = 0;
        opprs_indices = new int[world.getMap().getOppRespawnZone().length];
        for (Cell c : world.getMap().getOppRespawnZone()) {
            opprs_indices[ci] = fws.toInd(c.getRow(), c.getColumn());
            ci++;
        }
    }

    public boolean expectingBattle() {
        return world.getCurrentTurn() < 20;
    }

    public class OpHeroInf {
        int heroId;
        int dodge_rem;
        int special_rem;

        public OpHeroInf(Hero h) {
            this.heroId = h.getId();
            this.dodge_rem = 0;
            this.special_rem = 0;
        }
    }


    public void updateInfo() {
        for (Hero h : world.getOppHeroes()) {
            if (!oppHeros.keySet().contains(h.getId())) {
                oppHeros.put(h.getId(), new OpHeroInf(h));
            }
            OpHeroInf ohi = oppHeros.get(h.getId());
            if (ohi.special_rem > 0) ohi.special_rem--;
            if (ohi.dodge_rem > 0) ohi.dodge_rem--;
        }
        for (CastAbility ca : world.getOppCastAbilities()) {
            if (ca.getCasterId() == -1 || !oppHeros.keySet().contains(ca.getCasterId())) continue;
            Hero h = world.getHero(ca.getCasterId());
            OpHeroInf ohi = oppHeros.get(h.getId());
            if (ca.getAbilityName() == AbilityName.BLASTER_DODGE || ca.getAbilityName() == AbilityName.HEALER_DODGE || ca.getAbilityName() == AbilityName.GUARDIAN_DODGE || ca.getAbilityName() == AbilityName.SENTRY_DODGE) {
                ohi.dodge_rem = h.getDodgeAbilities()[0].getCooldown();
            } else {
                switch (h.getName()) {
                    case SENTRY: {
                        if (ca.getAbilityName() == AbilityName.SENTRY_RAY)
                            ohi.special_rem = h.getAbility(AbilityName.SENTRY_RAY).getCooldown();
                        break;
                    }
                    case GUARDIAN: {
                        if (ca.getAbilityName() == AbilityName.GUARDIAN_FORTIFY)
                            ohi.special_rem = h.getAbility(AbilityName.GUARDIAN_FORTIFY).getCooldown();
                        break;
                    }
                    case BLASTER: {
                        if (ca.getAbilityName() == AbilityName.BLASTER_BOMB)
                            ohi.special_rem = h.getAbility(AbilityName.BLASTER_BOMB).getCooldown();
                        break;
                    }
                    case HEALER: {
                        if (ca.getAbilityName() == AbilityName.HEALER_HEAL)
                            ohi.special_rem = h.getAbility(AbilityName.HEALER_HEAL).getCooldown();
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }


    public int[][] Ds(HeroActs[] hacts, int[] inds, int[] Dsgist) {
        int[][] Ds = new int[4][4];
        int[] x = new int[4];
        int[] y = new int[4];
        for (int i = 0; i < 4; i++) {
            if (hacts[i].h.getCurrentHP() == 0) {
                x[i] = 0;
                y[i] = 0;
                continue;
            }
            int nxtp = ((MvAction) hacts[i].acts.get(inds[i])).nxtpos;
            x[i] = nxtp / col_n;
            y[i] = nxtp % col_n;
        }
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                if (hacts[i].h.getCurrentHP() == 0 || hacts[j].h.getCurrentHP() == 0 || !(hacts[i].h.getCurrentCell().isInObjectiveZone() && hacts[i].h.getCurrentCell().isInObjectiveZone())) {
                    Ds[i][j] = 5;
                    Ds[j][i] = 5;
                    Dsgist[4]++;
                    continue;
                }
                Ds[i][j] = Math.abs(x[i] - x[j]) + Math.abs(y[i] - y[j]);
                Ds[j][i] = Ds[i][j];
                Dsgist[Math.min(4, Math.max(0, Ds[i][j] - 1))]++;
            }
        }
        return Ds;
    }

    public class dodge_stub {
        int h_ind;
        int tind;

        public dodge_stub(int wh_ind, int tind) {
            this.h_ind = wh_ind;
            this.tind = tind;
        }

        public DgAction makeDgAction() {
            return new DgAction(world.getMyHeroes()[h_ind], world.getMap().getCell(tind / world.getMap().getColumnNum(), tind % world.getMap().getColumnNum()), world, 100.0);
        }
    }


}
