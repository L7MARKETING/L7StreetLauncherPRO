# L7 Street Launcher PRO

Launcher Android com visual neon de rua (inspirado em GTA), sem root.

## Funcionalidades

- **Launcher padrão do Android** (categoria HOME): os botões e gestos de "Início" abrem o L7.
- **Gaveta de apps**: todos os apps instalados, com busca que ignora acentos e tecla "Ir" para abrir o primeiro resultado. Também mostra apps clonados e de perfil de trabalho, com selo.
- **Favoritos**: até 5 apps fixados na tela inicial. Para fixar ou remover, toque e segure o app.
- **Menu do app** (tocar e segurar): favoritar, ver informações do app (tela do Android) ou desinstalar.
- **HUD neon**: relógio grande (toque para abrir os alarmes), data (toque para abrir a agenda) e barra de bateria verde, amarela ou vermelha.
- **Papéis de parede**: Neon City (arte própria em vetor), imagem da galeria ou papel de parede do sistema.
- **6 cores neon**: Rosa Vice, Ciano, Verde Grove, Laranja Sunset, Roxo e Amarelo Táxi.
- **Ícones**: moldura neon ligada ou desligada, nomes visíveis ou ocultos, colunas automáticas ou fixas (4, 5 ou 6).
- **Acesso ao sistema**: botão "ANDROID" abre as Configurações originais; "Definir como launcher padrão" usa o diálogo oficial (Android 10+) ou a tela de apps padrão.
- **Gestos**: deslizar para cima abre a gaveta; segurar uma área vazia abre os Ajustes L7; "Voltar" fecha a gaveta.
- **Tela adaptável**: respeita notch/furo da câmera, barras de navegação por gestos ou botões, modo paisagem e tablets.

Requisitos: Android 8.0 ou superior. Sem root e sem permissões perigosas.

## Compilar pelo GitHub Actions (recomendado)

1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta, incluindo a pasta oculta `.github`.
2. Abra a aba **Actions**. O workflow **"Compilar APK - L7 Street Launcher PRO"** roda sozinho a cada push. Para rodar manualmente, use **Run workflow**.
3. Quando terminar (cerca de 4 a 6 minutos), abra a execução e baixe o artefato **L7-Street-Launcher-PRO-APK** (um .zip com os APKs).
   - `L7-Street-Launcher-PRO-debug.apk`: APK de teste, instalável.
   - `L7-Street-Launcher-PRO-release.apk`: versão otimizada, também instalável.
4. Para gerar uma Release com os APKs anexados, crie uma tag: `git tag v1.0.0 && git push origin v1.0.0`.

### Assinatura própria (opcional, para publicar)

Sem configuração, o APK de release é assinado com a chave de debug. Para usar sua própria chave, crie estes secrets em *Settings → Secrets and variables → Actions*:

| Secret | Conteúdo |
|---|---|
| `L7_KEYSTORE_BASE64` | o arquivo `.jks` em base64 (`base64 -w0 minha-chave.jks`) |
| `L7_KEYSTORE_PASSWORD` | senha do keystore |
| `L7_KEY_ALIAS` | alias da chave |
| `L7_KEY_PASSWORD` | senha da chave |

## Compilar no computador

Instale o Android Studio (ou o JDK 17 com o Android SDK) e rode:

```bash
./gradlew assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

## Instalar no celular

1. Copie o APK para o celular ou baixe direto pelo navegador.
2. Abra o APK pelo app **Arquivos**.
3. Se aparecer o aviso de fonte desconhecida, toque em **Configurações** e ative **Permitir desta fonte**. Depois volte e toque em **Instalar**.
4. Se o Play Protect avisar sobre "app desconhecido", toque em **Mais detalhes → Instalar mesmo assim**. O aviso aparece porque o app não veio da Play Store.

## Definir como launcher padrão

**Pelo próprio app (mais fácil):** abra o L7 e toque no aviso **"★ DEFINIR COMO LAUNCHER PADRÃO"**, ou vá em **AJUSTES L7 → Definir como launcher padrão**. Escolha **L7 Street Launcher PRO**.

**Pelas configurações:**

- **Realme (Realme UI):** Configurações → Apps → Apps padrão → App de tela inicial → L7 Street Launcher PRO. Em algumas versões o caminho é Configurações → Tela inicial e tela de bloqueio.
- **Android puro, Motorola, Xiaomi e outros:** Configurações → Apps → Apps padrão → App de início (ou "Tela inicial").
- **Samsung:** Configurações → Aplicativos → Escolher apps padrão → Tela inicial.

Outra opção: aperte o botão Início logo após instalar. Se o sistema perguntar qual tela inicial usar, escolha o L7 e toque em **Sempre**.

### Como voltar ao launcher original

Repita o caminho acima e escolha o launcher do fabricante (por exemplo, "Tela inicial do sistema"). Desinstalar o L7 também devolve o launcher original automaticamente.

### Observações para Realme e ColorOS

- Em algumas versões do Realme UI e ColorOS, a navegação por gestos fica limitada com launchers de terceiros, e o sistema pode voltar para os botões de navegação. Isso é uma restrição do fabricante, não do L7.
- Se o sistema fechar o launcher com frequência, desative a otimização de bateria do L7: Configurações → Bateria → Mais configurações / Otimizar uso → L7 Street Launcher PRO → Não otimizar.

## Estrutura do projeto

```
app/src/main/
├── AndroidManifest.xml          # HOME/LAUNCHER, queries de visibilidade (Android 11+)
├── java/com/l7/streetlauncher/
│   ├── MainActivity.kt          # tela inicial, HUD, favoritos, gaveta, gestos
│   ├── SettingsActivity.kt      # Ajustes L7
│   ├── AppRepository.kt         # lista de apps via LauncherApps (com perfis)
│   ├── AppAdapter.kt            # grade da gaveta + busca
│   ├── AppInfo.kt               # modelo de app + normalização de texto
│   ├── Prefs.kt                 # preferências locais
│   ├── WallpaperStore.kt        # importação/redução da imagem da galeria
│   ├── DefaultLauncher.kt       # verificar/solicitar launcher padrão
│   └── NeonStyle.kt             # paleta neon, estilos, insets, executores
└── res/                          # layouts, temas, ícone adaptativo, papel de parede vetorial
.github/workflows/android.yml     # build automático do APK
```

Versões: Gradle 8.11.1 · Android Gradle Plugin 8.7.3 · Kotlin 2.0.21 · compileSdk/targetSdk 35 · minSdk 26.
