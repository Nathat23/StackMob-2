package uk.antiperson.stackmob.events.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.tools.extras.GlobalValues;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Death implements Listener {

    private StackMob sm;

    public Death(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Entity dead = e.getEntity();

        if(!dead.hasMetadata(GlobalValues.METATAG)){
            return;
        }
        if(dead.getMetadata(GlobalValues.METATAG).get(0).asInt() <= 1){
            return;
        }

        int oldSize = dead.getMetadata(GlobalValues.METATAG).get(0).asInt();
        int subtractAmount = 1;


        if(sm.config.getCustomConfig().getBoolean("kill-all.enabled")){
            if (!sm.config.getCustomConfig().getStringList("kill-all.reason-blacklist")
                    .contains(dead.getLastDamageCause().getCause().toString())) {
                if (!sm.config.getCustomConfig().getStringList("kill-all.type-blacklist")
                        .contains(dead.getType().toString())) {
                    // Do it
                    multiplication(e.getEntity(), e.getDrops(), oldSize - 1, e.getDroppedExp());
                    if(sm.config.getCustomConfig().getBoolean("multiply-exp-enabled")){
                        e.setDroppedExp((int) Math.round((1.25 + ThreadLocalRandom.current().nextDouble(0.75)) * (oldSize - 1) * e.getDroppedExp()));
                    }
                    if(dead instanceof Slime){
                        sm.checks.spawnMoreSlime((Slime) dead, oldSize - 1);
                    }
                    return;
                }
            }
        }

        if(sm.config.getCustomConfig().getBoolean("kill-step.enabled")){
            if (!sm.config.getCustomConfig().getStringList("kill-step.reason-blacklist")
                    .contains(dead.getLastDamageCause().getCause().toString())) {
                if (!sm.config.getCustomConfig().getStringList("kill-step.type-blacklist")
                        .contains(dead.getType().toString())) {
                    int randomStep = ThreadLocalRandom.current().nextInt(1, sm.config.getCustomConfig().getInt("kill-step.max-step"));
                    if(randomStep >= oldSize){
                        subtractAmount = oldSize;
                    }else{
                        subtractAmount = randomStep;
                    }
                    multiplication(e.getEntity(), e.getDrops(), subtractAmount - 1, e.getDroppedExp());
                    if(sm.config.getCustomConfig().getBoolean("multiply-exp-enabled")){
                        e.setDroppedExp((int) Math.round((1.45 + ThreadLocalRandom.current().nextDouble(0.75)) * (subtractAmount - 1) * e.getDroppedExp()));
                    }
                    if(dead instanceof Slime){
                        sm.checks.spawnMoreSlime((Slime) dead, subtractAmount - 1);
                    }
                }
            }
        }

        if(oldSize != subtractAmount){
            Entity newe = sm.checks.duplicate(dead);
            newe.setMetadata(GlobalValues.METATAG, new FixedMetadataValue(sm, oldSize - subtractAmount));
            newe.setMetadata(GlobalValues.NO_SPAWN_STACK, new FixedMetadataValue(sm, true));
        }
        dead.removeMetadata(GlobalValues.METATAG, sm);
        dead.removeMetadata(GlobalValues.NO_STACK_ALL, sm);
        dead.removeMetadata(GlobalValues.NO_TASK_STACK, sm);
        dead.removeMetadata(GlobalValues.CURRENTLY_BREEDING, sm);
        dead.removeMetadata(GlobalValues.NOT_ENOUGH_NEAR, sm);
    }

    public void multiplication(LivingEntity dead, List<ItemStack> drops, int subtractAmount, int originalExperience){
        if(sm.config.getCustomConfig().getBoolean("multiply-drops.enabled")){
            if(dead.getKiller() != null){
                sm.dropTools.calculateDrops(drops, subtractAmount, dead.getLocation(), dead.getKiller().getItemInHand());
            }else{
                sm.dropTools.calculateDrops(drops, subtractAmount, dead.getLocation(), null);
            }
        }
    }
}
