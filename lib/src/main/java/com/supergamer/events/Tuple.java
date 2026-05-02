package com.supergamer.events;

class Tuple2<T,U>
{
    T value1;
    U value2;
    public Tuple2(T v1, U v2){
        value1 = v1;
        value2 = v2;
    }

    public T GetValue1(){return value1;}
    public U GetValue2(){return value2;}

    public void SetValue1(T value){value1 = value;}
    public void SetValue2(U value){value2 = value;}
}

class Tuple3<T,U,V>
{
    T value1;
    U value2;
    V value3;
    public Tuple3(T v1, U v2, V v3){
        value1 = v1;
        value2 = v2;
        value3 = v3;
    }

    public T GetValue1(){return value1;}
    public U GetValue2(){return value2;}
    public V GetValue3(){return value3;}

    public void SetValue1(T value){value1 = value;}
    public void SetValue2(U value){value2 = value;}
    public void SetValue3(V value){value3 = value;}
}