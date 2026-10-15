package qarma;

public class Vault {

    private Entry login;
    private Integer id;

    public Vault(Entry login, Integer id) {
        this.login = login;
        this.id = id;
    }

    public addEntry(Entry entry){


    }

    public empty(){}//	Creates a new vault with no entries (used by init)
    public load(path, master){} //	File → bytes → decrypt → JSON → list of Entry
    public save(path, master){}	//List of Entry → JSON → bytes → encrypt → file
    public add(entry){}	//Adds an entry, or replaces one for the same site
    public get(site){}	//Finds an entry by site
    public delete(site){}	//Removes an entry
    public list(){} //Returns all entries


}
