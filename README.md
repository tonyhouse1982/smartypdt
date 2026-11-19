# SmartyPDT (fork per Eclipse 2026-03)

Supporto Smarty per il PHP Development Tools di Eclipse, riportato in vita per **Eclipse IDE for PHP Developers 2026-03 (4.39.0)**.

Questo fork parte da https://github.com/pulse00/smartypdt e aggiorna la build:

- Tycho `5.0.4` (prima era 1.2.0)
- target platform `https://download.eclipse.org/releases/2026-03` (PDT 8.4.0)
- BREE / compilazione `JavaSE-21` (obbligatorio: PDT 8.4 richiede Java 21)
- corretto `ActionContributorForPhp` -> `ActionContributorForPHP` (rinominato in PDT)
- nuovo modulo `org.eclipse.php.smarty.repository` che genera il repository p2 installabile

## Requisiti build

- JDK 21+ (es. `brew install openjdk@21`)
- Maven 3.9+ (es. `brew install maven`)
- accesso a rete verso `download.eclipse.org` (la prima build risolve e scarica il target platform)

## Build

```bash
./build.sh
```

Genera `dist/smartypdt-update-site-0.11.0-<qualifier>.zip`, un archivio di update site p2.

## Installazione in Eclipse 2026-03

1. `Help` > `Install New Software...`
2. `Add...` > `Archive...` e selezionare `dist/smartypdt-update-site-*.zip`
3. Selezionare `SmartyPDT (Smarty support for PDT)` e completare l'installazione, poi riavviare Eclipse

In alternativa, scompattando lo zip in una cartella locale si puo' usare `Add...` > `Local...`.

## Cosa contiene

- `org.eclipse.php.smarty.core`: content type `*.tpl`/`*.template`, model handler WST, tokenizer/partitioner Smarty, builder DLTK, compilatore Smarty via PHP CLI
- `org.eclipse.php.smarty.ui`: editor Smarty, syntax highlighting, completion dei tag/espressioni Smarty, template, preference pages, azioni "Add/Remove Smarty Support..."

## Note

- Il progetto originale usava API interne di PDT/WST: molte sono ancora presenti in PDT 8.4, ma si tratta di API non garantite e possono cambiare in release future.
- Il plugin non e' firmato: Eclipse chiedera' conferma dell'editore non attendibile.
