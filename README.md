# KeyMapper - Mapeo de Teclado y Ratón para Android

Una aplicación Android que permite mapear teclado y ratón externos para controlar la pantalla táctil del dispositivo.

**Desarrollado por:** Michael Antonio Rodriguez Condega

## Características

- **Mapeo de teclado**: Asigna teclas del teclado externo a acciones de Android (Home, Back, Volume, etc.)
- **Control de ratón**: Mueve el cursor del ratón y traduce clicks a toques en pantalla
- **Scroll del ratón**: Convierte el scroll del ratón en scroll de pantalla
- **Detección automática** de dispositivos en `/proc/bus/input/devices`
- **Modos de inyección**:
  - **Root**: Usa `input` commands directamente con permisos root
  - **Shizuku**: Usa shell vía Shizuku para inyectar eventos
  - **Depuración inalámbrica**: Conecta vía ADB TCP para inyectar eventos
  - **Detección automática**: Prueba automáticamente el mejor método disponible

## Requisitos

- Android 6.0 (API 23) o superior
- Uno de los siguientes:
  - **Root** (Magisk, KernelSU, etc.)
  - **Shizuku** instalado y ejecutándose
  - **Depuración inalámbrica** habilitada

## Instalación

### Opción 1: Compilar desde código fuente
```bash
git clone https://github.com/MichaelARC-NI/KeyMapper.git
cd KeyMapper
# Compilar manualmente (ver build_manual/)
# O usar Android Studio / Gradle
```

### Opción 2: Instalar vía ADB
```bash
adb install -r -t --bypass-low-target-sdk-block keymapper.apk
```

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
- **Click izquierdo = Tocar**: Ejecuta un toque en la posición del cursor
- **Click derecho = Volver**: Ejecuta la acción de retroceso
- **Scroll = Scroll pantalla**: El scroll del ratón se traduce a scroll vertical

## Estructura del Proyecto

```
KeyMapper/
├── app/src/main/java/com/inputmapper/app/
│   ├── InputMapperApp.kt          # Application class
│   ├── MainActivity.kt            # UI principal
│   ├── service/
│   │   └── InputMapperService.kt  # Foreground service
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
│   └── xml/                       # USB device filter
└── build.gradle                   # Configuración Gradle
```

## Contacto

- **Facebook:** https://www.facebook.com/share/1EhxmtiyQN/
- **WhatsApp:** +505 8334 1349
- **Telegram:** @Michael_Antonio_Rodriguez
- **YouTube:** [AndroidMovil](https://youtube.com/@androidmovil)

## Licencia

MIT License
