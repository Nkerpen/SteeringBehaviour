# Trabalho II – Steering Behaviors (Prático) | SBCarsEngine

Implementação prática de agentes autônomos utilizando **Steering Behaviors** (Craig Reynolds) em Java 2D na engine `SBCarsEngine` (PUCPR – Inteligência Artificial e Ilusão de Inteligência em Jogos – Prof.ª Marina de Lara).

## 1. Como Executar no IntelliJ IDEA

1. Extraia o arquivo `SBCarsEngine_Completo.zip` em uma pasta do seu computador.
2. No **IntelliJ IDEA**, clique em **File → Open...** e selecione a pasta extraída (`SBCarsEngine`).
3. Caso o IntelliJ peça para selecionar o SDK do projeto, selecione qualquer **JDK 17 ou superior (ex.: JDK 17 ou JDK 21)** em **File → Project Structure → Project → SDK**.
4. Abra o arquivo `src/cars/engine/Window.java` e clique no ícone verde **Run (`▶`)** ao lado do método `main` (ou use a configuração pronta **Executar SBCarsEngine (Window)** no topo do IntelliJ).

## 2. Controles Interativos Durante a Execução

* **`[0]`** — **Mapa Completo (Padrão)**: Exibe múltiplos agentes autônomos executando as 3 combinações simultaneamente na pista e na arena.
* **`[1]`** — **Seek (Simples)**: Agente persegue continuamente a posição do cursor do mouse em velocidade máxima.
* **`[2]`** — **Flee (Simples)**: Agente foge do cursor do mouse assim que este entra no raio de pânico (`240 px`).
* **`[3]`** — **Arrive (Simples)**: Agente desloca-se até o ponto clicado (ou mouse) e desacelera suavemente dentro do raio de chegada (`220 px`) até parar completamente no raio de parada (`8 px`).
* **`[4]`** — **Wander (Simples)**: Dois agentes exploram o mapa de forma orgânica usando o círculo projetado à frente do veículo.
* **`[5]`** — **Obstacle Avoidance (Avançado)**: Agente atravessa o campo de obstáculos em direção ao mouse desviando de barreiras estáticas e outros carros usando caixa de detecção frontal dinâmica.
* **`[6]`** — **Path Following (Avançado)**: Dois agentes percorrem continuamente o circuito fechado de 8 waypoints respeitando o raio da pista (`38 px`).
* **`[7]`** — **Combinação 1 (Corredor Autônomo)**: `Path Following` (peso `1.0`) + `Obstacle Avoidance` (peso `2.4`) + `Arrive` opcional no Pit-Stop ao clicar próximo da pista (peso `1.3`).
* **`[8]`** — **Combinação 2 (Explorador Evasivo)**: `Wander` (peso `0.8`) + `Obstacle Avoidance` (peso `2.5`) + `Flee` do cursor do mouse (peso `1.8`).
* **`[9]`** — **Combinação 3 (Navegador Tático)**: `Arrive` ao clique/mouse (peso `1.1`) + `Obstacle Avoidance` (peso `2.3`).
* **`[D]`** — Alterna o **Modo Debug** (exibe vetores de velocidade e steering, sensores de colisão, círculo de Wander, projeção normal no Path e raios de Arrive/Flee).
* **`[Clique Esquerdo]`** — Define um ponto alvo fixo no mapa (usado no `Arrive`, `Combinação 1` e `Combinação 3`).
* **`[Clique Direito]` ou `[C]`** — Limpa o ponto alvo clicado.
* **`[R]`** — Reinicia as posições dos agentes do cenário atual.

## 3. Descrição Técnica dos Comportamentos Implementados (`SteeringBehaviors.java`)

1. **Seek (`seek`)**: Calcula o vetor direção até o alvo, normaliza e multiplica por `maxSpeed` para obter a `desiredVelocity`. Retorna `steering = desiredVelocity - velocity`.
2. **Flee (`flee`)**: Calcula o vetor na direção oposta à ameaça (`position - threat`). Quando a distância é menor que `panicDistance` (`240 px`), aplica a velocidade máxima de afastamento escalonada pela proximidade.
3. **Arrive (`arrive`)**: Mede a distância $d$ até o alvo. Fora do `slowingRadius` (`220 px`), comporta-se como `Seek`. Dentro do `slowingRadius`, reduz linearmente a velocidade desejada ($v_{\text{desired}} = v_{\max} \cdot \frac{d}{r_{\text{slowing}}}$) com ganho de frenagem calibrado para respeitar a distância física de parada ($v_{\max}^2 / 2a_{\max}$) e parar suavemente dentro do `stopRadius` (`8 px`).
4. **Wander (`wander`)**: Projeta um círculo de raio `48 px` a uma distância de `92 px` à frente da direção atual do carro. A cada quadro, incrementa o ângulo interno com ruído suave (`jitter = 4.5 rad/s * dt`) e executa `Seek` para o ponto correspondente na circunferência.
5. **Obstacle Avoidance (`obstacleAvoidance`)**: Projeta uma caixa de sensor frontal cujo alcance varia dinamicamente com a velocidade atual (`65 px` a `165 px`). Transforma a posição dos obstáculos estáticos (`Obstacle`) e dos carros vizinhos (`world.getNeighbors()`) para o espaço local do veículo (`localX` longitudinal via produto escalar com `heading`, e `localY` lateral via produto escalar com `side = perp(heading)`). Identifica o obstáculo mais próximo com interseção positiva e aplica uma força lateral proporcional à penetração e proximidade + força de frenagem longitudinal.
6. **Path Following (`pathFollowing`)**: Projeta a posição futura do agente (`pos + vel * 0.42s`), encontra o ponto normal mais próximo nos segmentos do circuito fechado (`Path.findClosestPoint`) e define um alvo `62 px` à frente no sentido da pista. Se a distância até a linha central superar o raio da pista (`38 px`), aplica `Seek` integral para retornar ao traçado; dentro da pista, aplica guiagem suave para manter o fluxo contínuo nas curvas.

## 4. Combinações e Justificativa dos Pesos (`StudentCar.java`)

* **Combinação 1 – Corredor Autônomo (`Path Following [1.0]` + `Obstacle Avoidance [2.4]` + `Arrive Pit-Stop [1.3]`)**:
  * **Justificativa**: O comportamento base mantém o carro percorrendo o circuito (`1.0`). Quando um obstáculo na pista ou outro carro mais lento entra no sensor frontal, o peso maior de `Obstacle Avoidance` (`2.4`) aliado à redução temporária da tração da pista (`0.45`) permite que o agente contorne a barreira ou faça a ultrapassagem com segurança e retorne suavemente ao traçado logo depois.
* **Combinação 2 – Explorador Evasivo (`Wander [0.8]` + `Obstacle Avoidance [2.5]` + `Flee [1.8]`)**:
  * **Justificativa**: Em condições normais, o agente explora o cenário de forma fluida (`Wander = 0.8`). Ao aproximar o cursor do mouse dentro de `240 px`, a força de `Flee` (`1.8`) assume o comando para afastar o veículo rapidamente da ameaça, enquanto `Obstacle Avoidance` (`2.5`) tem prioridade máxima para impedir colisões com obstáculos durante a exploração ou fuga.
* **Combinação 3 – Navegador Tático (`Arrive [1.1]` + `Obstacle Avoidance [2.3]`)**:
  * **Justificativa**: Permite comandar o agente para qualquer ponto clicado no mapa, contornando obstáculos intermediários (`2.3`) e estacionando suavemente no destino final (`1.1`).
