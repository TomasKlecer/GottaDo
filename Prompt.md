## GottaDo appka
Delam apku v androidStudiu v kotlinovi pro android 7+ velice podobnou aplikaci Stuff todo widget, hlavní 2 komponenty jsou widget a samotná aplikace, která slouží jen k nastavení chování a zobrazení widgetu.

Napřed si vytvoř 1-5 souborů (napriklad Etape1.md - Etape5.md) s etapami, kde si vždy anglicky popíšeš architektinický návrh jednotlivých etap po celcích, ve kterých se ti to bude dobře kódit. Pak tě vždycky nechám nakódovat každou etapu zvlášť. Dej bacha ať na sebe etapy navazují a dávají dohromady smysl.

## Widget
- Widget je poloprůhledný čtverec s posuvným/scrolling seznamem Kategorií a záznamů. Kategorie je vždy napsaná tučným písmem trochu větší velikostí a pod každou jsou záznamy.
- každý záznam má před sebou barevný puntík (jako odrážka) a je to vlastně řádek - přestože může zobrazovat i víceřádkový text
- při kliknutí na záznam se zobrazí možnost editovat záznam
- Při kliknutí na řádek s kategorii se zobrazí modál přidání nového záznamu (spíše se automaticky vytvoří prázdný záznam a zobrazí se modál s možností jeho editace a akorát nebude vidět tlačítko cancel a bude tam jen save a delete)
- při kliknutí a držení na radku se zatnamem se záznam označí jako splněný (font zešedne a záznam se přeškrtne) a splněný se označí naopak jako nesplněný
- při kliknutí na pravý nebo levý okraj widgetu (dle nastavení dálě) se otevre popup který umožní posouvat zaznamy a po kliknutí mimo zmizí. bude možné přesouvat záznamy - táhnutím se záznam postupně swapuje s řádky widgetu (Takto je možné posunout záznam i do jiné kategorie)
- úplně na konci slide window pod všem kategoriemi je tlačítko které otevře Appku samotnou a umožní editovat nastavení a tlačítko na posouvání(pokud je vypnuté posouvání při kliknutí na okraj) - tyto tlačítka nejdou vidět pokud člověk nesescrolluje až dolů v seznamu aby nezabíraly místo (jsou tedz jakoby sou48st9 scrollovacího okna)

**editace záznamu**
- text záznamu - editovatelne zvyrazneni, barva, kurziva i pro části textu
- completed flag
- barva puntíku/checkboxu
- čas (záznamy s časem se budou automaticky řadit v dané kategorii)
- category dropdown - pro snadnější přesun záznamu mezi kategoriemi
- Dole tlačítka Save, Cancel, Delete (vyhodí redo popup tlačítko dole na widgetu)



## Appka
Appka umožňuje spravovat jednotlivé widgety - je tedy možné mít více různých widgetů - například na jednom budu mít spíše poznámky, na dalším denní tasky a na dalším tracking progressu za týden a podle toho jednotlivým widgetům nastavím kategorie. Kategorie jsou společné pro všechny widgety, takže pokud si v jednom widgetu vytvořím kategorii poznámky, ve druhým si ji taky budu moct zobrazit
**Nastavení celého widgetu**
- Nadpis a popisek widgetu - když jsou prázdné, nejdou vidět
- poznámka - vůbec nejde vidět ve widgetu a je jen pro lepší orientování se v appce
- Barva a průhlednost pozadí widgetu (color pie + slider)
- velikost písma kategorie
- velikost písma záznamu
- defaultní barva písma
- posloupnost kategorií ve widgetu a jestli jsou ve widgetu vidět
- Dropdown na výběr posuvníku záznamů (Vlevo, Vpravo, vůbec)

- NOTE: programovat tak, aby to šlo rozšířit o změny stylů(jakože to cele vypada jako notýsek například) nebo nastavování pozadí widgetu a fontu pisma

**Nastavení jednotlivých kategorií**
- jména kategorií
- checkbox jsou v kategorii záznamy s časem před záznamy bez času nebo naopak?
- checkbox jestli zobrazovat u záznamů checkbox místo puntíku
- možnost přidat kategorii

**Denní, týdenní, měsíční a roční rutiny jednotlivých kategorií:**
- rutiny nad tasky kategorií, co se provádí vždy za nějaký čas
- nastavení v jaký čas (u týdenních i den, u měsíčních  kolikátýho, u ročních datum) se rutina provede
- visibility routine: dropdown hidden/visible a od-do(pro denní čas, pro týdenní den atp...)
- co se má stát s nedokončenými tasky
- co se má stát s dokončenými tasky
- pro obě varianty co se ma stat s tasky je tam checkbox delete
- pokud je delete false, je tam navíc checkbox complete/uncomplete a dropdown přesunout do s možnostmi "nepřesouvat"+všechny ostatní kategorie
- pokud je pro ně vybráno cokoliv krom smazat, zobrazí se ještě možnost přesunout do jiné kategorie+výběrník do jaké



**Speciální typ kategoríí**
- tyto kategorie maji stejne chovani jako klasicke, jenom maji specifické vlastnosti navíc a budou se také explicitně synchronizovat s kalendářem po implementaci rozšíření
- kategorie dnes - vlastně obyčejné chování + sync s kalendářem
- zítra - všechny taky se přesunou do today
- sunday, monday, tuesday ...., kopiruji se z ni zaznamy 

**Nastavení záznamů**
- Umožní povolit co všechno se bude zobrazovat v nastavení záznamů - nekdo to bude treba chtít víc clean a tak si tam da jen moznost plain textu bez editace a jenom checkbox completed atp.


**koš záznamů**
ukazuje historii smazaných zýznamů prehledne rozdelene podle dní a umožňuje je obnovit, je možné ho vysypat, nebo mazat jednotlive zaznamy


## ROZSIRENI
zatím přímo neimplementuj, ale počítej s nimi a přizpůsob architekturu tak, aby šly snadno přidat

**Sync s google kalendářem**
- Do kategorii se budou kopírovat tasky z google kalendáře
- tlacitko sync, nastavení sync every: hour/day+hour/week+day+hour
- přidání rutin které umožní syncovat se s kalendářem tak, že tam bude možnost sync with: today, tomorrow, next sun/mon/tue/... atp.


**precreated widgets**
- vlastně jen přidá data bez vytváření nových funkcionalit. predvytvorene widgety a notifikace pro typicky usecases (typicky denní productivity todo, týdení plánovač, denník, měsíční tasky, workout planner atp) Aby nový uživatel mohl rovnou využívat apku a nemusel nic nstavovat složitě 

**koš jako denník**
- lepší zobrazení a filtrování koše, aby sloužil jako časosběr toho co se dělo

**notifikace**
- natavení notifikací když se nesplní tasky

**lepsi posuvnik**
- přidá následující možnost do nastavení widgetu v appce:
- Checkbox ActivateOnTap - FALSE: pokud uživatel klikne na oblast posuvníku, automaticky bude moci posouvat řádek, TRUE: po kliknutí na oblast posuvníku se otevre popup který umožní pouze posouvat zaznamy a po kliknutí mimo zmizí.
- NOTE: jestli to nejde z principu widgetu, tak na to kašli


## Jak programovat?

Programuj v anglictine a komentare taky nechavej jen v anglictine. Snaž se to držet znovupoužitelný a rozšiřitelný.

Use MVVM architecture.
Use Room.
Use WorkManager for routines.
Support multiple widget instances.
Target Android 7+.
Assume widget limitations (RemoteViews constraints).

The widget must not contain business logic.
It should only render state provided by repository/usecases.

Every user action must go through a UseCase in domain layer.

Each widget instance has its own configuration stored separately and linked by widgetId.

Routines operate on categories, not on widgets.

Since RemoteViews do not support real drag & drop, simulate moving tasks via overlay activity that visually represents drag and then updates order in database.

Store formatted text as HTML string.

Use WorkManager with unique periodic work for routine processing. Routine worker must:
query routines
apply business rules
update tasks
trigger widget refresh