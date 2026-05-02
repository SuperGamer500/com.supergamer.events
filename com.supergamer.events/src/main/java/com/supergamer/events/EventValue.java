package com.supergamer.events;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class EventValue<T>
{
    private boolean isActive = false;
    //The value of the object
    private volatile T value;
    //A certain amount of threads are created to handle events
    private  static EventThread[] threads = new EventThread[5];
    private EventThread myThread;
    //This is used to check if any expressions that a thread sends to be add can be added to this event object
    private final  EventExpression<T> type = new EventExpression<>(null, null);

    //Contains all of the events that are linked to objects
    private final ArrayList<EventExpression<T>> expressions = new ArrayList<>();

    //Tries to add an event by checking compatibility
    private <K> boolean AttemptAddExpression(EventExpression<K> eventExpression)
    {
        if(!eventExpression.getClass().isInstance(type)) return false;
        expressions.add((EventExpression<T>)eventExpression);
        return true;

    }

    public static void CloseTasks(){
        for(int i = 0; i < threads.length; i++){
            if(threads[i] != null)
                threads[i].interrupt();
        }
    }
    //Returns a thread for an event value to use the idea is that the smallest thread will be picked first
    private static EventThread AssignThread()
    {
        if(threads[0] == null)
        {
            threads[0] = new EventThread();
            threads[0].start();
            return threads[0];
        }
        EventThread smallestThread = null;
        for(int i = 0 ; i < threads.length; i++)
        {
            EventThread cThread = threads[i];
            if(smallestThread == null)
            {
                smallestThread = cThread;
                continue;
            }
            if(cThread == null)
            {
                threads[i] = new EventThread();
                threads[i].start();
                return threads[i];
            }
            if(cThread.eventsList.size() < smallestThread.eventsList.size())
                smallestThread = cThread;
        }

        return smallestThread;
    }

    private static boolean  RemoveThread(EventThread thread)
    {
        for(int i = 0 ; i < threads.length; i++)
        {
            if(threads[i] == thread){
                threads[i].interrupt();
                threads[i] = null;
                return true;
            }
        }
        return false;
    }


    //Handles all executions of events
    static private class EventThread extends Thread
    {
        private ArrayList<EventValue<?>> eventsList = new ArrayList<>();
        private BlockingQueue<Tuple2<EventValue<?>,?>> valueQueue = new LinkedBlockingQueue<>();
        public BlockingQueue<Tuple2<EventValue<?>,EventExpression<?>>> addListenerQueue = new LinkedBlockingQueue<>();
        public BlockingQueue<Tuple2<EventValue<?>,?>> removeListenerQueue = new LinkedBlockingQueue<>();


        public void run()
        {
            while (true) {
                try{

                    //Wait for a value change
                    Tuple2<EventValue<?>,?> mostRecent = valueQueue.take();

                    //Attempt to add all new listeners to their respective event objects
                    if(!addListenerQueue.isEmpty())
                    {
                        addListenerQueue.forEach(x -> x.value1.AttemptAddExpression(x.value2));
                        addListenerQueue.clear();
                    }
                    //Attempt to remove listeners from their respective event objects
                    if(!removeListenerQueue.isEmpty())
                    {
                        removeListenerQueue.forEach(x -> x.value1.expressions.removeIf(y -> y.obj.get() == x.value2));
                        removeListenerQueue.clear();
                    }

                    mostRecent.value1.Cycle(mostRecent.value2);
                    //Remove all objects that are no longer being used that are linked to events
                    removeListenerQueue.forEach(x -> x.value1.expressions.removeIf(y -> y.obj.get() == null));



                }
                catch(InterruptedException e){
                    System.out.println(e.getMessage());
                    eventsList.forEach(x ->x.Dispose());
                    RemoveThread(this);
                    return;
                }
            }
        }

    }

    //Clean up any refrences to the thread or events
    public void Dispose(){
        isActive = false;
        myThread = null;
    }

    public boolean  SetValue(T newValue){
        if(!isActive) return false;
        return myThread.valueQueue.offer(new Tuple2<>(this,newValue));
    }
    public T GetValue(){
        return value;
    }



    public EventValue(T startingValue) {

        myThread = AssignThread();
        value = startingValue;
        isActive = true;
    }

    //Handles adding new events
    public boolean AddListener(WeakReference<?> obj,ExpressionMethod<T> newEvent)
    {
        if(!isActive) return false;
        return  myThread.addListenerQueue.offer(new Tuple2<>(this,new EventExpression<>(obj,newEvent)));
    }
    public boolean AddListener(Object obj,ExpressionMethod<T> newEvent)
    {
        return AddListener(new WeakReference<>(obj), newEvent);
    }

    //Removes events
    public boolean  RemoveListener(Object obj){
        if(!isActive) return false;
        return myThread.removeListenerQueue.offer(new Tuple2<>(this,obj));

    }

    //Calls all events assotiated with this object if the value sent is of the right type
    private  <K> void Cycle(K valueSent)throws InterruptedException
    {
        if(!value.getClass().isInstance(valueSent))return;
        value = (T)valueSent;

        for(EventExpression<T> expression : expressions)
            expression.event.OnChange(value);
    }


    public interface ExpressionMethod<T>{
        void  OnChange(T newValue) throws InterruptedException;
    }

    static private class EventExpression<T>
    {
        final WeakReference<?> obj;
        final ExpressionMethod<T> event;
        public EventExpression(WeakReference<?> obj, ExpressionMethod<T> event){
            this.obj = obj;
            this.event = event;
        }
    }
}

