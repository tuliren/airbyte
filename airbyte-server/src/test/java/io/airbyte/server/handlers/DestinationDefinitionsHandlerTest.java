/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.server.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.airbyte.api.model.DestinationDefinitionIdRequestBody;
import io.airbyte.api.model.DestinationDefinitionRead;
import io.airbyte.api.model.DestinationDefinitionReadList;
import io.airbyte.api.model.DestinationDefinitionUpdate;
import io.airbyte.commons.json.JsonValidationException;
import io.airbyte.config.StandardDestination;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.config.persistence.ConfigRepository;
import io.airbyte.server.validators.DockerImageValidator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DestinationDefinitionsHandlerTest {

  private DockerImageValidator dockerImageValidator;
  private ConfigRepository configRepository;
  private StandardDestination destination;
  private DestinationDefinitionsHandler destinationHandler;

  @BeforeEach
  void setUp() {
    configRepository = mock(ConfigRepository.class);
    dockerImageValidator = mock(DockerImageValidator.class);
    destination = generateDestination();
    destinationHandler = new DestinationDefinitionsHandler(configRepository, dockerImageValidator);
  }

  private StandardDestination generateDestination() {
    final UUID destinationId = UUID.randomUUID();

    return new StandardDestination()
        .withDestinationId(destinationId)
        .withName("presto")
        .withDockerImageTag("12.3")
        .withDockerRepository("repo")
        .withDocumentationUrl("https://hulu.com");
  }

  @Test
  void testListDestinations() throws JsonValidationException, IOException, ConfigNotFoundException, URISyntaxException {
    final StandardDestination destination2 = generateDestination();

    when(configRepository.listStandardDestinations()).thenReturn(Lists.newArrayList(destination, destination2));

    DestinationDefinitionRead expectedDestinationDefinitionRead1 = new DestinationDefinitionRead()
        .destinationDefinitionId(destination.getDestinationId())
        .name(destination.getName())
        .dockerRepository(destination.getDockerRepository())
        .dockerImageTag(destination.getDockerImageTag())
        .documentationUrl(new URI(destination.getDocumentationUrl()));

    DestinationDefinitionRead expectedDestinationDefinitionRead2 = new DestinationDefinitionRead()
        .destinationDefinitionId(destination2.getDestinationId())
        .name(destination2.getName())
        .dockerRepository(destination2.getDockerRepository())
        .dockerImageTag(destination2.getDockerImageTag())
        .documentationUrl(new URI(destination2.getDocumentationUrl()));

    final DestinationDefinitionReadList actualDestinationDefinitionReadList = destinationHandler.listDestinationDefinitions();

    assertEquals(
        Lists.newArrayList(expectedDestinationDefinitionRead1, expectedDestinationDefinitionRead2),
        actualDestinationDefinitionReadList.getDestinationDefinitions());
  }

  @Test
  void testGetDestination() throws JsonValidationException, ConfigNotFoundException, IOException, URISyntaxException {
    when(configRepository.getStandardDestination(destination.getDestinationId()))
        .thenReturn(destination);

    DestinationDefinitionRead expectedDestinationDefinitionRead = new DestinationDefinitionRead()
        .destinationDefinitionId(destination.getDestinationId())
        .name(destination.getName())
        .dockerRepository(destination.getDockerRepository())
        .dockerImageTag(destination.getDockerImageTag())
        .documentationUrl(new URI(destination.getDocumentationUrl()));

    final DestinationDefinitionIdRequestBody destinationDefinitionIdRequestBody = new DestinationDefinitionIdRequestBody()
        .destinationDefinitionId(destination.getDestinationId());

    final DestinationDefinitionRead actualDestinationDefinitionRead = destinationHandler.getDestinationDefinition(destinationDefinitionIdRequestBody);

    assertEquals(expectedDestinationDefinitionRead, actualDestinationDefinitionRead);
  }

  @Test
  void testUpdateDestination() throws ConfigNotFoundException, IOException, JsonValidationException {
    when(configRepository.getStandardDestination(destination.getDestinationId())).thenReturn(destination);
    DestinationDefinitionRead currentDestination =
        destinationHandler.getDestinationDefinition(new DestinationDefinitionIdRequestBody().destinationDefinitionId(destination.getDestinationId()));
    final String currentTag = currentDestination.getDockerImageTag();
    final String currentRepo = currentDestination.getDockerRepository();
    final String newDockerImageTag = "averydifferenttag";
    assertNotEquals(newDockerImageTag, currentTag);

    final DestinationDefinitionRead sourceRead = destinationHandler.updateDestinationDefinition(
        new DestinationDefinitionUpdate().destinationDefinitionId(this.destination.getDestinationId()).dockerImageTag(newDockerImageTag));

    assertEquals(newDockerImageTag, sourceRead.getDockerImageTag());
    verify(dockerImageValidator).assertValidIntegrationImage(currentRepo, newDockerImageTag);
  }

}