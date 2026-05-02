package com.supergamer.events;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class TaskTimer
{
    public static TaskTimer instance = new TaskTimer();
    public static long SecondsToMiliSeconds(long value){return value*1000;}
    public static long MinutesToMiliSeconds(long value){return value*60000;}
    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    private TimerThread timerThread = new TimerThread();

    private TaskTimer(){
        scheduledExecutorService.scheduleAtFixedRate(timerThread,0,1, TimeUnit.MILLISECONDS);
    }


     public void AddTask(Object reference,long waitTime, EventValue.ExpressionMethod<Float> progressEvent)
    {
        EventValue<Float> eventValue = new EventValue<>(0f);
        eventValue.AddListener(reference,progressEvent);
        timerThread.addQueue.offer(new TimerElement(new WeakReference<>(reference),eventValue,waitTime));
    }
    private class TimerThread implements Runnable
    {
        public BlockingQueue<TimerElement> addQueue = new LinkedBlockingQueue<>();
        private Set<TimerElement> elements = new HashSet<>();

        public void run()
        {

            while (!addQueue.isEmpty()){
                try{
                    elements.add(addQueue.take());
                }
                catch (InterruptedException e){}

            }
            elements.forEach(x -> {
                x.currentElapsedTime += 1;
                float progress = x.progress();
                x.GetEvent().SetValue(progress);
                if(progress >= 1)
                    x.OnEnd();
            });
            elements.removeIf(x -> (x.progress() >= 1 || x.GetWeakReference().get() == null));
        }
    }
    private class TimerElement
    {
        long currentElapsedTime;
        private Tuple3<WeakReference<?>,EventValue<Float>,Long> tuple3;

        float progress(){return (float)currentElapsedTime/GetTime();};
        public TimerElement(WeakReference<?> reference, EventValue<Float> progressEvent, Long lng){
            tuple3 = new Tuple3<>(reference,progressEvent,lng);
        }
        public WeakReference<?> GetWeakReference(){return tuple3.GetValue1();}
        public EventValue<Float> GetEvent(){return tuple3.GetValue2();}
        public Long GetTime(){return tuple3.GetValue3();}
        public void OnEnd(){Dispose();}
        public void Dispose(){tuple3.value2.Dispose();}
    }

    public void Close(){
        scheduledExecutorService.shutdownNow();
    }
}
