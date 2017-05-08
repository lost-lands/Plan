package main.java.com.djrapitops.plan.data.cache.queue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.Phrase;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.Settings;
import main.java.com.djrapitops.plan.data.UserData;
import main.java.com.djrapitops.plan.database.Database;

/**
 *
 * @author Rsl1122
 */
public class DataCacheSaveQueue {

    private BlockingQueue<UserData> q;
    private SaveSetup s;

    /**
     *
     * @param plugin
     * @param clear
     */
    public DataCacheSaveQueue(Plan plugin, DataCacheClearQueue clear) {
        q = new ArrayBlockingQueue(Settings.PROCESS_SAVE_LIMIT.getNumber());
        s = new SaveSetup();
        s.go(q, clear, plugin.getDB());
    }

    /**
     *
     * @param data
     */
    public void scheduleForSave(UserData data) {
        Log.debug(data.getUuid()+": Scheduling for save");
        try {
            q.add(data);
        } catch (IllegalStateException e) {
            Log.error(Phrase.ERROR_TOO_SMALL_QUEUE.parse("Save Queue", Settings.PROCESS_SAVE_LIMIT.getNumber() + ""));
        }
    }

    /**
     *
     * @param data
     */
    public void scheduleForSave(Collection<UserData> data) {
        Log.debug("Scheduling for save: "+data.stream().map(u -> u.getUuid()).collect(Collectors.toList()));
        try {
            q.addAll(data);
        } catch (IllegalStateException e) {
            Log.error(Phrase.ERROR_TOO_SMALL_QUEUE.parse("Save Queue", Settings.PROCESS_SAVE_LIMIT.getNumber() + ""));
        }
    }

    /**
     *
     * @param data
     */
    public void scheduleNewPlayer(UserData data) {
        Log.debug(data.getUuid()+": Scheduling new Player");
        try {
            q.add(data);
        } catch (IllegalStateException e) {
            Log.error(Phrase.ERROR_TOO_SMALL_QUEUE.parse("Save Queue", Settings.PROCESS_SAVE_LIMIT.getNumber() + ""));
        }
    }

    /**
     *
     * @param uuid
     * @return
     */
    public boolean containsUUID(UUID uuid) {
        return new ArrayList<>(q).stream().map(d -> d.getUuid()).collect(Collectors.toList()).contains(uuid);
    }

    /**
     *
     */
    public void stop() {
        if (s != null) {
            s.stop();
        }
        s = null;
        q.clear();
    }
}

class SaveConsumer implements Runnable {

    private final BlockingQueue<UserData> queue;
    private Database db;
    private DataCacheClearQueue clear;
    private boolean run;

    SaveConsumer(BlockingQueue q, DataCacheClearQueue clear, Database db) {
        queue = q;
        this.db = db;
        this.clear = clear;
        run = true;
    }

    @Override
    public void run() {
        try {
            while (run) {
                consume(queue.take());
            }
        } catch (InterruptedException ex) {
        }
    }

    void consume(UserData data) {
        if (db == null) {
            return;
        }
        UUID uuid = data.getUuid();
        Log.debug(uuid+": Saving: "+uuid);
        try {
            db.saveUserData(uuid, data);
            data.stopAccessing();
            Log.debug(uuid+": Saved!");
            if (data.shouldClearAfterSave()) {
                if (clear != null) {
                    clear.scheduleForClear(uuid);
                }
            }
        } catch (SQLException ex) {
//            queue.add(data);
            Log.toLog(this.getClass().getName(), ex);
        }
    }

    void stop() {
        run = false;
        if (db != null) {
            db = null;
        }
        if (clear != null) {
            clear = null;
        }
    }
}

class SaveSetup {

    private SaveConsumer one;
    private SaveConsumer two;

    void go(BlockingQueue<UserData> q, DataCacheClearQueue clear, Database db) {
        one = new SaveConsumer(q, clear, db);
        two = new SaveConsumer(q, clear, db);
        new Thread(one).start();
        new Thread(two).start();
    }

    void stop() {
        one.stop();
        two.stop();
    }
}
