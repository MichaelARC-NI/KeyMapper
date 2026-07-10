# KeyMapper - Mapeo de Teclado y Ratón para Android

**Desarrollado por:** Michael Antonio Rodriguez Condega

## Características

- **Mapeo de teclado** completo con dialogo de configuracion
- **Control de ratón** con sensibilidad ajustable
- **3 modos de inyección**: Root, Shizuku, ADB inalambrico
- **Conexion ADB inalambrica** con dialogo de emparejamiento (IP + Puerto + Codigo)
- **Deteccion automatica** de dispositivos en `/proc/bus/input/devices`
- **Scroll del ratón** como scroll de pantalla
- **UI optimizada** para pantallas grandes

## Requisitos

- Android 6.0 (API 23) o superior

## Instalación

### Via ADB
```bash
adb install -r -t --bypass-low-target-sdk-block keymapper.apk
```

### Configuracion ADB Inalambrica
1. Abre Ajustes > Desarrollador > Depuracion inalambrica
2. Empareja el dispositivo con codigo de 6 digitos
3. Abre KeyMapper > Selecciona "Depuracion Inalambrica"
4. Toca "Configurar Conexion ADB..."
5. Ingresa IP y Puerto de conexion
6. Toca CONECTAR

## Mapeo de Teclas por Defecto

| Tecla Fuente | Accion |
|-------------|--------|
| F5 / Home | Home |
| Backspace / Escape | Back |
| Page Up / Page Down | Vol+ / Vol- |
| Tab / Enter | Tab / Enter |
| F1 / F2 / F3 | Home / Back / Recent |

## Redes Sociales

- **Facebook:** https://www.facebook.com/share/1EhxmtiyQN/
- **WhatsApp:** +505 8334 1349
- **Telegram:** @Michael_Antonio_Rodriguez
- **YouTube:** [AndroidMovil](https://youtube.com/@androidmovil)
