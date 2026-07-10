# KeyMapper - Mapeo de Teclado y Ratón para Android

Una aplicación Android que permite mapear teclado y ratón externos para controlar la pantalla táctil del dispositivo.

## Características

- **Mapeo de teclado**: Asigna teclas del teclado externo a acciones de Android (Home, Back, Volume, etc.)
- **Control de ratón**: Mueve el cursor del ratón y traduce clicks a toques en pantalla
- **Scroll del ratón**: Convierte el scroll del ratón en scroll de pantalla
- **Múltiples métodos de inyección**:
  - **Root**: Usa `input` commands directamente con permisos root
  - **Shizuku**: Usa ADB shell vía Shizuku para inyectar eventos
  - **Depuración inalámbrica**: Conecta vía ADB TCP para inyectar eventos
  - **Detección automática**: Prueba automáticamente el mejor método disponible

## Requisitos

- Android 8.0 (API 26) o superior
- Uno de los siguientes:
  - **Root** (Magisk, KernelSU, etc.)
  - **Shizuku** instalado y ejecutándose
  - **Depuración inalámbrica** habilitada

## Instalación

### Opción 1: Compilar desde código fuente
```bash
git clone https://github.com/TU_USUARIO/KeyMapper.git
cd KeyMapper
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Opción 2: Instalar vía ADB
```bash
adb install keymapper.apk
```

## Configuración

### Root
Simplemente instala la app y concede permisos root cuando se solicite.

### Shizuku
1. Instala Shizuku desde: https://shizuku.rikka.app/download/
2. Inicia Shizuku (wireless debugging o ADB)
3. Abre KeyMapper y concede permisos de Shizuku

### Depuración Inalámbrica
1. Habilita Depuración de desarrollador en Ajustes
2. Activa Depuración inalámbrica
3. Empareja el dispositivo con el código de emparejamiento
4. Abre KeyMapper y selecciona "Depuración Inalámbrica"

## Mapeo de Teclas por Defecto

| Tecla Fuente | Acción |
|-------------|--------|
| F5 / Home | Home |
| Backspace / Escape | Back |
| Page Up | Volumen + |
| Page Down | Volumen - |
| Tab | Tab |
| Enter | Enter |
| F1 | Home |
| F2 | Back |
| F3 | Recent Apps |

## Configuración del Ratón

- **Sensibilidad**: Ajusta la velocidad del cursor (0% a 200%)
- **Invertir eje Y**: Invierte el movimiento vertical
- **Click izquierdo = Tocar**: El click izquierdo ejecuta un toque en la posición del cursor
- **Click derecho = Volver**: El botón derecho ejecuta la acción de retroceso
- **Scroll = Scroll pantalla**: El scroll del ratón se traduce a scroll vertical

## Estructura del Proyecto

```
KeyMapper/
├── app/src/main/java/com/inputmapper/app/
│   ├── InputMapperApp.kt          # Application class
│   ├── MainActivity.kt            # UI principal
│   ├── service/
│   │   ├── InputMapperService.kt  # Foreground service
│   │   └── ShizukuInputService.kt # Shizuku binder service
│   ├── model/
│   │   ├── InputMode.kt           # Modos de inyección
│   │   └── KeyMapping.kt          # Modelos de datos
│   ├── injection/
│   │   ├── InputInjector.kt       # Interfaz de inyección
│   │   ├── RootInjector.kt        # Inyección vía root
│   │   ├── ShizukuInjector.kt     # Inyección vía Shizuku
│   │   └── AdbInjector.kt         # Inyección vía ADB
│   ├── receiver/
│   │   └── BootReceiver.kt        # Auto-start en boot
│   └── util/
│       ├── ShellExecutor.kt       # Ejecución de comandos
│       └── DeviceDiscovery.kt     # Descubrimiento de dispositivos
├── app/src/main/res/
│   ├── layout/                    # Layouts XML
│   ├── values/                    # Strings, colors, themes
│   ├── xml/                       # USB device filter
│   └── drawable/                  # Iconos
└── build.gradle.kts               # Configuración Gradle
```

## Permisos

- `BLUETOOTH` / `BLUETOOTH_CONNECT`: Para dispositivos Bluetooth
- `FOREGROUND_SERVICE`: Para el servicio de mapeo
- `POST_NOTIFICATIONS`: Para la notificación del servicio
- `MANAGE_EXTERNAL_STORAGE`: Para acceso completo al almacenamiento

## Licencia

MIT License
