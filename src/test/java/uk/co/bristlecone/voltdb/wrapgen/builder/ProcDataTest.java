package uk.co.bristlecone.voltdb.wrapgen.builder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import uk.co.bristlecone.voltdb.wrapgen.source.ProcReturnType;
import uk.co.bristlecone.voltdb.wrapgen.source.RunParameter;

public class ProcDataTest {
  private final static String TEST_NAME = "testName";
  private final static List<RunParameter> TEST_PARAMETERS = ImmutableList.of(RunParameter.of("testType", "testName"));
  private final static ProcReturnType TEST_RETURN_TYPE = ProcReturnType.VOLTABLE_ARRAY;
  private final static String TEST_PACKAGE_NAME = "test.package";

  @Test
  public void builderInstantiatesClassCorrectly() {
    ProcData testee = new ProcData.Builder().setName(TEST_NAME)
        .setParameters(TEST_PARAMETERS)
        .setReturnType(TEST_RETURN_TYPE)
        .setPackageName(TEST_PACKAGE_NAME)
        .build();
    assertThat(testee.name(), is(equalTo(TEST_NAME)));
    assertThat(testee.parameters(), is(equalTo(TEST_PARAMETERS)));
    assertThat(testee.returnType(), is(equalTo(TEST_RETURN_TYPE)));
    assertThat(testee.packageName(), is(equalTo(TEST_PACKAGE_NAME)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void builderThrowsOnNullName() {
    new ProcData.Builder().setName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void builderThrowsOnEmptyName() {
    new ProcData.Builder().setName("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void builderThrowsOnNullParameters() {
    new ProcData.Builder().setParameters(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void builderThrowsOnNullReturnType() {
    new ProcData.Builder().setReturnType(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void builderThrowsIfAnyFieldsAreNotSet() {
    new ProcData.Builder().build();
  }
}