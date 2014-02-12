package com.gpl.rpg.AndorsTrail.model.io;

import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.actor.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Gabriel on 11/02/14.
 */
public class PlayerWriter {

    RangeWriter rangeWriter = new RangeWriter();

    public void writeToParcel(Player player, DataOutputStream dest) throws IOException {
        dest.writeInt(player.baseTraits.iconID);
        dest.writeInt(player.baseTraits.maxAP);
        dest.writeInt(player.baseTraits.maxHP);
        dest.writeUTF(player.name);
        dest.writeInt(player.moveCost); // TODO: Should we really write this?
        dest.writeInt(player.baseTraits.attackCost);
        dest.writeInt(player.baseTraits.attackChance);
        dest.writeInt(player.baseTraits.criticalSkill);
        dest.writeFloat(player.baseTraits.criticalMultiplier);
        rangeWriter.writeToParcel(player.baseTraits.damagePotential,dest);
        dest.writeInt(player.baseTraits.blockChance);
        dest.writeInt(player.baseTraits.damageResistance);
        dest.writeInt(player.baseTraits.moveCost);

        rangeWriter.writeToParcel(player.ap, dest);
        rangeWriter.writeToParcel(player.health, dest);
        player.position.writeToParcel(dest);
        dest.writeInt(player.conditions.size());
        for (ActorCondition c : player.conditions) {
            c.writeToParcel(dest);
        }
        player.lastPosition.writeToParcel(dest);
        player.nextPosition.writeToParcel(dest);
        dest.writeInt(player.level);
        dest.writeInt(player.totalExperience);
        player.inventory.writeToParcel(dest);
        dest.writeInt(player.baseTraits.useItemCost);
        dest.writeInt(player.baseTraits.reequipCost);
        dest.writeInt(player.skillLevels.size());
        for (int i = 0; i < player.skillLevels.size(); ++i) {
            dest.writeInt(player.skillLevels.keyAt(i));
            dest.writeInt(player.skillLevels.valueAt(i));
        }
        dest.writeUTF(player.spawnMap);
        dest.writeUTF(player.spawnPlace);
        dest.writeInt(player.questProgress.size());
        for(Map.Entry<String, HashSet<Integer> > e : player.questProgress.entrySet()) {
            dest.writeUTF(e.getKey());
            dest.writeInt(e.getValue().size());
            for(int progress : e.getValue()) {
                dest.writeInt(progress);
            }
        }
        dest.writeInt(player.availableSkillIncreases);
        dest.writeInt(player.alignments.size());
        for(Map.Entry<String, Integer> e : player.alignments.entrySet()) {
            dest.writeUTF(e.getKey());
            dest.writeInt(e.getValue());
        }
    }

}
