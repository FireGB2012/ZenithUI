# ZenithUI

Gabriel builds this app by talking to Claude, on more than one PC. Claude's own memory stays on the
PC it was made on; what is here travels with the repository.

## Where things are

- `README.md`: what the app does and how it is put together, including "Working on another PC".
- `specs/`: the design of each piece of work, with what was decided and why.
- `RELEASING.md`: signing, the Play checklist, and "Turning the server on".
- `server.properties`: where Zenith's server is. It is tracked, so every PC builds the same app.
  `server/` holds the server program and `PROMPT-FOR-SERVER-AI.md`, the message it is installed
  from. That message carries the program and its checksum: changing `server/zenith_server.py`
  means a new message and a reinstall on the server, so it is not changed in passing.

## Gabriel's rules

- Finish a piece of work by committing and pushing it. Work on a branch; merge to `main` only when
  he asks.
- Never run `gradlew connectedDebugAndroidTest`: it uninstalls the debug app afterwards, and that
  wiped his setup once. Install the test APK and run it with `adb shell am instrument`.
- The phone and the emulator he looks at are his. Do not tap around on them, and never uninstall
  Zenith from them to get an install through. Before installing on his phone, copy its
  `shared_prefs/zenith.xml` out with `run-as`.
- Signing files (`*.jks`, `*.keystore`, `keystore.properties`) are never committed, and their
  passwords are never printed.
- On the real server, make only throwaway accounts and delete them afterwards.

## Checking work

`./gradlew :app:testDebugUnitTest :app:assembleDebug`. A rule gets a unit test before its code.
`ZenithServerTest` starts the real server program and needs Python. To try the app against a server
without touching the real one: `python server/zenith_server.py`, then build with
`-PzenithServerUrl=http://10.0.2.2:8787` and install on an emulator.
