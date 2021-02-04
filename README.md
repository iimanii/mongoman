# Minimalistic mongodb framework

Mongoman is an easy to use framework that maps your java classes to mongodb objects


# Installation

Download dist/mongoman.jar and include it in your libraries (or compile your own version)

Must also include mongo-java-driver in your project

# Usage

#### Loading and storing objects into mongodb
- Implement a class that **extends** mongoman.Base
- Your class will represent one unique collection in the database
- Collection name must be passed via @Kind annotation with the class
- Only **non-static public** fields get stored
- **non-static final public** fields are used as keys to load and store the objects and marked as unique index (they cannot repeat)
- Use load() and store() to load and save the object
- Your class must implement a 0-argument constructor

##### Example

```
@Kind("car")
class Car extends Base {
    public int myInteger;               // stored in db
    public double[] myDoubleArray;      // stored in db
   
    public final String myId;   // saved in db, also unique across collection
    private double priv;        // this will not get stored in db

    public Car(...) {
        myId = ....
        .....
    }
    
    // must implement a 0-argument constructor
    public Car() {
        ....
    }
        
}
```

```
Datastore store = new Datastore(....);
store.setDefaultService(store);

Car c = new Car(..);
c.load();
c.myInteger = 20;
c.save();
```

#### Supported types
Not all fields can be serialized into mongodb, the following are the ones guaranteed to work

- All primitive types (int, byte, float, ...)
- All primitive type arrays (int[], byte[], float[], ....)
- String
- java.util.List
- java.util.Set
- java.util.Map
- java.util.Date
- Any class that extends mongoman.Base
- An array of any class that extends mongoman.Base
- java.util.Map<String, ? extends mongoman.Base>

Note that using when using java.util.Set, 2 Base objects are considered equal if their keys are equal (final fields)
if you wish to change this behavior you must implement "equals" and "hashCode" for your class

#### Keys
Mongoman uses keys to load and save objects, keys are composed of the mongo _id field plus any **final** field defined in the object

Usage of keys is implicit within mongoman framework and is just mentioned here for clarity


#### Working with nested objects
Mongoman allows the usage of nested Base classes

``` public Class Door extends Base {...}```
``` 
public Class Car extends Base {
    Door door;
    ....
}
```


Nested objects will be stored as "keys" in the parent object, if you want the full object to be saved with parent (this might be useful for querying) add **FullSave** annotation to the field

``` 
@Kind("car")
public Class Car extends Base {
    @FullSave()
    Door door;
    ....
    
    public Car(...) {
        super();
        ....
    }
}
```

When loading and saving the parent object, Mongoman can automatically load / save any nested objects in their respective collections, while only storing "keys" in the original object

To do so use **.load(true)** and **.save(true)**

#### Using references
It is possible have 2 classes referencing each other, just make sure to set the @Reference annotation on one of them to avoid cycles while loading.
@Reference objects will not get loaded when fully loading an object with **.load(true)**

``` 
@Kind("car")
public Class Car extends Base {
    Door door;
    ....
    
    public Car(...) {
        ....
    }
}

@Kind("door")
public Class Door extends Base {
    @Reference
    Car car;
    ....
    
    public Door(...) {
        ....
    }
}
```

#### Shallow Objects
It is possible to enforce that an object will never get saved in its own collection.
This can be common with nested objects that are fully saved in parent

```
@Kind(value = "door", shallow = true)
public Class Door extends Base {
    ....
    
    public Door(...) {
        ....
    }
}
```

```
    Door d = new Door(...)
    ....
    d.save();           // this will always fail because the object is defined as shallow
```

### Options
Use ***@Options*** annotation when defining a class to do further tweeking
***ignoreNull*** does store null fields into database when saving
***ignoreUnknownProperties*** remove extra/obsolete properties found in database object and are not defined in the class
```
@Kind("door")
@Options(ignoreNull = true, ignoreUnknownProperties = true)
public Class Door extends Base {
    ....
    
    public Door(...) {
        ....
    }
}
```



#### Query
You can query the database using the **Query** class, all mongodb filters are supported

```
    MongoClient mongoClient = new MongoClient(....);
    Datastore store = new Datastore(mongoClient, ...);
    Datastore.setDefaultService(store);

    Filter _gt = new Filter("name", Query.FilterOperator.EQUALS, "....");
    Filter _ni = new Filter("type", Query.FilterOperator.NOT_IN, new int[]{1, 3});
    Filter _eq = new Filter("door.type", Query.FilterOperator.EQUALS, 2);
    Filter _rx = new Filter("door.tag, Query.FilterOperator.REGEX, "*alloy");
    _rx.options("i");

    Filter _and = new Filter(Query.FilterOperator.AND, _gt, _ni, _eq, _rx);

    Query<Car> query = new Query<>(Car.class);
    query.setFilter(f);
        
    Cursor<Car> cursor = q.execute();

    while(cursor.hasNext()) {
        Car car = cursor.next();
        ...
    }
```

One can use Base objects in query, in this case it will be translated into its key
```
    Filter _eq = new Filter("door", Query.FilterOperator.EQUALS, door0);
```

#### Unique Fields
You can mark some fields as Unique by adding the @Unique annotation to ensure uniqueness across the DB

``` 
@Kind("car")
public Class Car extends Base {
    Door door;

    @Unique
    long code;
    ....
}
```

#### Atomicty / Transactions
Currently not implemented, will be included in future versions
