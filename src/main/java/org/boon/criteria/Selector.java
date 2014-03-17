/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.criteria;


import org.boon.core.Function;
import org.boon.core.reflection.BeanUtils;
import org.boon.core.reflection.fields.FieldAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.boon.Lists.list;
import static org.boon.Boon.joinBy;

public abstract class Selector {


    /**
     * Performs the actual selection from the results.
     * @param selectors list of selectors
     * @param results results we are selecting from
     * @param fields fields
     * @param <ITEM> List of items
     * @return map of selected items per row
     */
    public static <ITEM> List<Map<String, Object>> performSelection( List<Selector> selectors, List<ITEM> results, Map<String, FieldAccess> fields ) {
        List<Map<String, Object>> rows = new ArrayList<>( results.size() );


        for ( Selector s : selectors ) {
            s.handleStart( results );
        }


        int index = 0;
        for ( ITEM item : results ) {
            Map<String, Object> row = new LinkedHashMap<>();
            for ( Selector s : selectors ) {
                s.handleRow( index, row, item, fields );
            }
            index++;
            rows.add( row );
        }

        for ( Selector s : selectors ) {
            s.handleComplete( rows );
        }

        return rows;
    }


    /** Create a list of selectors.
     *
     * @param selects array of selectors
     * @return
     */
    public static List<Selector> selects( Selector... selects ) {
        return list( selects );
    }

    protected String name;

    protected String alias;

    protected final boolean path;


    /**
     * Is this a property path?
     * @param prop property
     * @return true or false
     */
    private static boolean isPropPath(String prop) {
        if (prop.contains(".")) return true;
        if (prop.equals("this")) return true;
        if (prop.contains("[")) return true;

        return false;
    }

    /**
     * Default constructor.
     */
    public Selector() {
        path = false;
    }


    /**
     *
     * @param nameOrPath name of property path
     *
     * @param alias alias of property path
     */
    public Selector( String nameOrPath, String alias ) {
        name = nameOrPath;
        this.alias = alias == null ? nameOrPath : alias;
        path = isPropPath(name);
    }


    /**
     *
     * @param nameOrPath name of property path
     *
     */
    public Selector( String nameOrPath ) {
        name = nameOrPath;
        alias = name;
        path = isPropPath(name);
    }


    public String getName() {
        return name;
    }


    protected void getPropertyValueAndPutIntoRow(Map<String, Object> row, Object item, Map<String, FieldAccess> fields) {
        if (!path && fields!=null) {
            row.put( this.alias, fields.get( this.name ).getValue( item ) );
        } else {
            row.put( this.alias, BeanUtils.atIndex(item, name)  );
        }
    }


    protected Object getPropertyValue(Object item, Map<String, FieldAccess> fields) {
        if (!path && fields!=null ) {
            return fields.get( this.name ).getValue( item );
        } else {
            return BeanUtils.atIndex(item, name);
        }
    }


    /**
     * Allows you to select a property or property path.
     * @param propName name of property or property path
     * @return returns a Selector
     */
    public static Selector select( final String propName ) {
        return new Selector( propName, propName ) {

            @Override
            public void handleRow( int index, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {

                getPropertyValueAndPutIntoRow(row, item, fields);
            }


            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    /**
     * Selects but allows having a different alias for the output.
     * @param propName name of property or property path to select
     * @param alias name that the value will be selected as.
     * @return selector
     */
    public static Selector selectAs( final String propName, final String alias ) {
        return new Selector( propName, alias ) {

            @Override
            public void handleRow( int index, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {


                getPropertyValueAndPutIntoRow(row, item, fields);
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }



    /**
     * Selects but allows having a different alias for the output.
     * @param propName name of property or property path to select
     * @param alias name that the value will be selected as.
     * @param transform Function that allows you to convert from an object into another object
     * @return selector
     * */
    public static Selector selectAs( final String propName, final String alias,
                                     final Function transform) {
        return new Selector( propName, alias ) {

            private final boolean path = isPropPath(propName);
            @Override
            public void handleRow( int index, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {

                if (!path && fields!=null) {
                    row.put( this.name, transform.apply(fields.get( this.name ).getValue( item )) );
                } else {
                    row.put( alias, transform.apply(BeanUtils.atIndex( item, propName ))  );
                }
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    public static Selector toStr( final String name ) {
        return new Selector( name + ".toString()", null ) {
            @Override
            public void handleRow( int index, Map<String, Object> row, Object item, Map<String, FieldAccess> fields ) {
                Object selected = fields.get( this.name ).getValue( item );
                row.put( this.name, selected == null ? "" : selected.toString() );
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    public static Selector toStr() {
        return new Selector( "toString()", null ) {
            @Override
            public void handleRow( int index, Map<String, Object> row, Object item, Map<String, FieldAccess> fields ) {
                row.put( this.name, item.toString() );
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    @Deprecated
    public static Selector select( final String... ppath ) {
        return new Selector( joinBy( '.', ppath ), null ) {
            int index = 0;

            @Override
            public void handleRow( int rowNum, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {

                Object o = BeanUtils.getPropByPath( item, ppath );


                row.put( this.name, o );
            }


            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    @Deprecated
    public static Selector toStr( final String... ppath ) {
        return new Selector( joinBy( '.', ppath ) + ".toString()", null ) {
            int index = 0;

            @Override
            public void handleRow( int rowNum, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {

                Object o = BeanUtils.getPropByPath( item, ppath );


                row.put( this.name, o == null ? "" : o.toString() );
            }


            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    @Deprecated
    public static Selector selectPropPath( final String... ppath ) {
        return new Selector( joinBy( '.', ppath ), null ) {
            @Override
            public void handleRow( int rowNum, Map<String, Object> row,
                                   Object item, Map<String, FieldAccess> fields ) {

                Object o = BeanUtils.getPropByPath( item, ppath );

                row.put( this.name, o );
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }

    /**
     * Just grabs the index of the result set.
     * @return
     */
    public static Selector rowId() {

        return new Selector( "rowId", null ) {
            @Override
            public void handleRow( int index, Map<String, Object> row, Object item, Map<String, FieldAccess> fields ) {
                row.put( name, index );
            }

            @Override
            public void handleStart( List<? extends Object> results ) {
            }

            @Override
            public void handleComplete( List<Map<String, Object>> rows ) {
            }
        };
    }





    public abstract void handleRow( int index, Map<String, Object> row,
                                    Object item,
                                    Map<String, FieldAccess> fields );

    public abstract void handleStart( List<? extends Object> results );

    public abstract void handleComplete( List<Map<String, Object>> rows );

    public String getAlias() {
        return alias;
    }
}
