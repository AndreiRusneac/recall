# Putting Recall online for a pilot

This sets up one small server that runs the app, its database and an HTTPS front door.
Everything is tested locally (build, HTTPS, login, backup and restore), but it has not been run on a
real server yet: expect to fix small things on the first real run.

## Choosing where to run it

Pick any provider that gives you a **Linux virtual server in an EU region**. For two or three
clinics that is the simplest and usually cheapest option, and the data stays in the EU.
Check the current price and region list on the provider's own pricing page before you decide.

What to look for:
- At least **2 GB of memory** (the app is a Java program and the database needs room too).
- An **EU location**, because patients' appointment data is personal data.
- Snapshots or backups of the whole server as a second safety net, if offered.

A managed platform (where you push code and it runs) means less server work, but costs more and
the database backup and region settings differ per vendor. If you prefer that, the `Dockerfile` in
the repository root runs there too; see "Running it for a pilot clinic" in the README.

## One-time setup

You need: a server (Ubuntu 22.04 or 24.04 is assumed), a domain name you control, and an SSH key.

1. **Domain.** Create a DNS `A` record, for example `recall.example.com`, pointing at the server's
   IP address. Wait until it resolves (`dig +short recall.example.com`).
2. **Log in and secure the server.**
   ```
   ssh root@SERVER_IP
   adduser deploy && usermod -aG sudo deploy      # work as "deploy", not root
   # copy your SSH key to the deploy user, then disable password login in /etc/ssh/sshd_config
   ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw enable
   apt update && apt install -y unattended-upgrades apache2-utils   # security updates, and htpasswd
   ```
3. **Install Docker** from the official instructions at https://docs.docker.com/engine/install/ubuntu/
   (this includes the `docker compose` plugin), then `sudo usermod -aG docker deploy` and log in again.
4. **Get the code and configure it.**
   ```
   git clone https://github.com/AndreiRusneac/recall.git && cd recall/deploy
   cp .env.example .env
   nano .env        # set DOMAIN and a long random DB_PASSWORD (openssl rand -base64 24)
   ```
5. **Start it.**
   ```
   docker compose up -d --build
   docker compose ps          # all three should be "running"
   ```
   The first start takes a few minutes. Caddy gets the HTTPS certificate by itself. Open
   `https://your-domain` and you should see the login page.
6. **Give a clinic its account** (there is no sign-up page):
   ```
   cd ~/recall
   RECALL_DB_CONTAINER=$(cd deploy && docker compose ps -q db) \
     scripts/create-clinic.sh "Clinic name" owner@clinic.example "a-strong-password"
   ```
   Give the clinic the address, the email and the password, and ask them to keep it private.
7. **Switch on nightly backups.** Run `crontab -e` and add:
   ```
   30 3 * * *  /home/deploy/recall/deploy/backup.sh >> /home/deploy/recall-backup.log 2>&1
   ```
   Then **copy `deploy/backups/` somewhere else** regularly (another server or a storage box).
   A backup that lives only on the server is lost with the server.
   **Do a restore test once**, so you know it works before you need it (steps in `backup.sh`).

## Everyday operations

| Task | Command (from `~/recall/deploy`) |
|---|---|
| Update to the latest version | `git pull && docker compose up -d --build` |
| See what the app is doing | `docker compose logs -f app` |
| Restart | `docker compose restart app` |
| Back up now | `./backup.sh` |

Database changes (migrations) are applied automatically when the app starts.
Set up a free uptime check (any monitoring service that pings a URL) on `/login` so you hear
about an outage before a clinic does.

## Things to know

- **Login lockouts reset on restart.** After 5 wrong passwords the account is locked for
  15 minutes; restarting the app clears that.
- **The `dev` profile must never be used here.** It loads demo data and a demo login with a
  published password. The compose file does not set it.
- **Reminders are still sent by a person tapping a button** (decision 002). Nothing is sent by the
  server by itself.

## Before real patients' data goes in (not legal advice)

Appointments at a clinic can reveal health information, which GDPR treats as a special category.
Before the pilot starts, talk to a lawyer or the clinic's data protection contact about at least:
- a **data processing agreement** between you and each clinic, and one between you and your hosting provider;
- how the clinic **records patients' consent** to receive WhatsApp messages (the app only records
  the answer: the clinic is responsible for asking);
- how long data is kept and how a patient's data is **deleted** on request (there is no delete
  button yet, it is done in the database).
