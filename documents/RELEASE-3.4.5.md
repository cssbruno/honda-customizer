# 3.4.5 — escrita serial XP direta, manual

Adiciona **Serial XP direta — teste manual** à tela do perfil exato `0x4012A`. O botão requer conexão identificada, ausência de operação pendente e confirmação de veículo estacionado. Ao abrir, encerra a conexão Binder deste app e descarta os valores anteriores.

A tela inteira permite informar porta, velocidade atual e corpo hexadecimal do pacote XP. Revisar mostra os bytes reais `2E + corpo + checksum`; alterar qualquer campo invalida a revisão. O envio usa uma biblioteca JNI própria e faz uma única escrita direta na UART, sem passar pelo serviço SYU. Resultado e contagem de bytes ficam na tela e no histórico da auditoria em memória.

## Limites concretos

- O caminho e a ligação física da UART precisam ser identificados pelo operador. Não há porta ou baud padrão. Desconectar o app não encerra o SYU nem libera suas portas; a confirmação de disponibilidade é manual, não uma detecção de exclusividade.
- A UART deve estar configurada para a velocidade informada, saída sem transformações e 8N1 sem controle de fluxo. A implementação verifica e preserva essa configuração; não chama `tcsetattr`, não altera permissões, não usa root, não reinicia serviços e não lê a porta.
- A abertura pode falhar por permissões/SELinux. Escritas parciais ou interrompidas não são repetidas. Bytes aceitos pelo kernel não confirmam transmissão física nem efeito no veículo.
- A rota corresponde à saída UART do encoder XP; **não é a porta MCU que exige o envelope `88 55`**. Não há seleção automática entre essas rotas.
- Ainda não há formato XP comprovado para transportar arbitrariamente ID CAN + dados OEM. Esta versão não adiciona layouts/cores extras do cluster nem requer flash.
- Android mínimo agora é **5.0 / API 21**, por exigência do NDK utilizado. A central Android 10 atende esse requisito. Bibliotecas ARM32, ARM64, x86 e x86_64 incluídas.

## Evidência do smali

Referência SYU `2.23.0711.1001`, SHA-256 `4b428302e29c9e2503ccf7844a127f5bed59450736317629aa42b5eb35a9b577`; a central fotografada usa `2.23.0718.1700`, ainda não inspecionada.

- `g0/w.j0`: low word `0x12A` seleciona `g0/r`.
- `g0/r.f`: gera `E9 2E + corpo + (soma(corpo) XOR FF)`, com truncamento a byte.
- `g0/a.c`: na saída configurada `c1/w`, remove o seletor externo `E9` antes de encaminhar os bytes.
- `c/d$b.f`, `c/d`, `i1/c0`: escrita serial por `JniSerial`; configuração 8 bits, paridade N, 1 stop bit. A implementação nova preserva a configuração existente em vez de reconfigurar a porta.
- `i0/e4.d`, `chip/Chip.m`: seleção/configuração de portas dependente da plataforma. Não prova qual UART está ligada à XP instalada.

## Validação

- 104 testes JUnit aprovados, lint sem erros, APK release compilado para quatro ABIs.
- 21 testes Python existentes aprovados.
- Três verificações JNI no computador com pseudoterminal: bytes exatos e configuração preservada; velocidade divergente rejeitada sem bytes; arquivo comum preservado e rejeitado.
- Pseudoterminal e fixtures não estão no APK e não comprovam compatibilidade com o hardware XP. Nenhum comando foi enviado ao veículo.
