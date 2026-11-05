# DevilsVault — evidenza funzionale E2E

Eseguito il **5 novembre 2026**, sul branch
`devin/1793895608-java21-boot3-mongodb`, PR #1.
Ambiente reale locale: Java 21 / Spring Boot 3.5.16 / MongoDB 8 replica set;
interfaccia JSP invariata, nessun mock.

## Ambiente e verifica

- Applicazione: http://localhost:8080/CSE545-SS/login
- Mailpit: http://localhost:8025 (OTP non necessario per questo importo).
- Lo stack Docker Compose era già avviato: app, mongo, legacy-mysql e mailpit
  attivi; migrator terminato con codice 0. Nessun reset o modifica diretta dei dati.
- `mvn -B verify` eseguito con JDK 21: **exit 0, BUILD SUCCESS**.
  Output integrale: [mvn-verify.log](mvn-verify.log).
- Maven usa il mirror già configurato
  `https://maven-central-eu.storage-download.googleapis.com/maven2/`.

Per avviare lo stesso ambiente dalla root:

```sh
MAVEN_MIRROR_URL=https://maven-central-eu.storage-download.googleapis.com/maven2 docker compose up -d --build
```

## Account e operazione

Solo account dimostrativi del seed, password comune **`Passw0rd@`**:

| Ruolo | Username | Uso |
|---|---|---|
| Customer | alice@devilsvault.com | Mittente, CHECKING 1001 |
| Manager | manager@devilsvault.com | Approvazione, approver 2 |
| Customer | bob@devilsvault.com | Destinatario, CHECKING 1004 |

Bonifico esterno: **250**, da **1001:CHECKING** a **Bob Bianchi:1004**,
descrizione **E2E demo transfer**, ID assegnato **3**.
Logout tramite il menu utente prima di ogni cambio account; logout anche
al termine. Il limite è una sessione per utente.

## Passi e risultati osservati

1. Login Alice: dashboard con CHECKING 1001 = **5000.00** e SAVINGS 1002 =
   **2500.00**.
2. Transfer Money → Someone's Account. Compilati destinatario, conto mittente,
   importo 250 e descrizione; Submit una sola volta.
3. Conferma **“Your payment was successful!”**, Transfer To 1004,
   Transfer From 1001-CHECKING, Amount 250. Nessuna richiesta OTP (<1000).
4. Lettura MongoDB: ID 3 pending, saldo Alice ancora 5000.00 con hold 250.00;
   saldo Bob ancora 1200.00 con hold 0.00.
5. Logout Alice, login manager, Transaction: ID 3 visibile con sender 1001,
   receiver 1004, amount 250, critical false. Selezionato Approve e Submit.
6. Conferma **“Transaction processed successfully!”**, tabella pending vuota;
   ID 3 nella tabella completati con status approve e approver 2.
7. Logout manager, login Alice: dashboard **4750.00**, voce DEBIT 250,
   descrizione E2E demo transfer, status approve.
8. Logout Alice, login Bob: dashboard **1450.00**, voce CREDIT 250 con
   stessa descrizione e status approve.
9. Lettura finale MongoDB: hold entrambi 0.00, ID 3 completed/approve,
   nessuna copia pending. Somma dei saldi invariata: **6200.00**.

| Conto | Prima: saldo / hold | In attesa: saldo / hold | Dopo: saldo / hold |
|---|---|---|---|
| Alice CHECKING 1001 | 5000.00 / 0.00 | 5000.00 / 250.00 | 4750.00 / 0.00 |
| Bob CHECKING 1004 | 1200.00 / 0.00 | 1200.00 / 0.00 | 1450.00 / 0.00 |

Snapshot di lettura: [prima](balances-before.txt),
[pending](pending-ledger.txt), [dopo](balances-after.txt).
Il saldo disponibile di Alice durante l'attesa è 5000 − 250 = 4750;
il saldo contabile scende solo dopo approvazione.

## Video e screenshot sequenziali

- [Registrazione annotata MP4](transfer-approval.mp4), circa 60 secondi:
  dashboard iniziale, bonifico, login manager, approvazione e saldi finali.
  Il login iniziale Alice è stato completato prima della registrazione;
  la schermata login è documentata separatamente.
- [Versione animata WebP](transfer-approval.webp), 2 fps / larghezza 640 px.

| File | Contenuto |
|---|---|
| [01-login.png](01-login.png) | Pagina login |
| [02-alice-before.png](02-alice-before.png) | Saldo iniziale Alice |
| [03-transfer-form.png](03-transfer-form.png) | Bonifico compilato |
| [04-transfer-confirmation.png](04-transfer-confirmation.png) | Conferma bonifico |
| [05-manager-dashboard.png](05-manager-dashboard.png) | Dashboard manager |
| [06-manager-approve.png](06-manager-approve.png) | ID 3, Approve selezionato |
| [07-approval-complete.png](07-approval-complete.png) | Approvazione completata |
| [08-alice-after.png](08-alice-after.png) | Alice 4750.00 |
| [09-bob-after.png](09-bob-after.png) | Bob 1450.00 |

### Alice: confronto prima / dopo

| 🔴 Prima — 5000.00 | 🟢 Dopo — 4750.00 |
|---|---|
| ![Alice prima](https://devin-gtm.devinenterprise.com/attachments/8509ffcd-38dd-4e5e-9192-356f2a13bae6/02-alice-before.png) | ![Alice dopo](https://devin-gtm.devinenterprise.com/attachments/0e987526-480a-4a64-968e-f839ee831378/08-alice-after.png) |

### Approvazione e accredito

| Manager — ID 3 approvato | Bob — 1450.00 |
|---|---|
| ![Approvazione](https://devin-gtm.devinenterprise.com/attachments/30677ee9-f6ed-4d8f-81f2-5c6bf443e4ba/07-approval-complete.png) | ![Bob dopo](https://devin-gtm.devinenterprise.com/attachments/51308bb9-8bd1-4610-a0ea-ab75dc4e0f88/09-bob-after.png) |

## Anomalie e limiti

- Il widget reCAPTCHA mostra **“Localhost is not in the list of supported
  domains for this site key.”** Il login con credenziali valide funziona
  comunque; la protezione reCAPTCHA non è verificata. Warning del login
  legacy, non attribuito qui alla migrazione.
- Durante il setup un primo tentativo di login è fallito per input errato
  nella tastiera virtuale; ripetuto con la password corretta, login riuscito.
  Cliccare **Accept** nella tastiera virtuale prima di **Login**.
- Nessun malfunzionamento osservato nel bonifico / approvazione richiesti.
- Non eseguiti i flussi opzionali OTP >=1000 e login regular con modifica
  indirizzo. Nessuna verifica completa di tutti gli altri ruoli o flussi.
- Dati finali lasciati nello stack per ispezione; tutte le sessioni UI chiuse
  tramite Logout. File di evidenza lasciati nella working tree, non committati
  né pubblicati da questo agente.
